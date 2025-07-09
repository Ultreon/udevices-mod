package dev.ultreon.devicesnext.firmware;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import dev.ultreon.devicesnext.firmware.api.VEfi;
import io.github.kawamuray.wasmtime.*;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.wasi.WasiCtx;
import io.github.kawamuray.wasmtime.wasi.WasiCtxBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VEfiApplication extends Thread implements AutoCloseable {

    public static final Logger LOGGER = LoggerFactory.getLogger("Devices:Firmware");
    private final byte[] data;
    private final List<WasmProxy> proxies;
    private Store<Void> store;
    private Linker linker;
    private Module module;
    private WasiCtx wasi;
    private Binding<Void> binding;
    private Memory mem;
    private Func fn;

    public VEfiApplication(byte[] data, String name, WasmProxy... proxies) {
        this.data = data;
        this.proxies = List.of(proxies);
        this.setName("WasmExecutor:" + name);
        this.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Error in thread: {}", t.getName(), e));
    }

    public VEfiApplication(byte[] data, Collection<WasmProxy> proxies) {
        this.data = data;
        this.proxies = List.copyOf(proxies);
    }

    @ApiStatus.Internal
    public void run() {
        // Configure the initial compilation environment, creating the global
        // `Store` structure. Note that you can also tweak configuration settings
        // with a `Config` and an `Engine` if desired.
        System.err.println("Initializing...");

        AtomicReference<Memory> memory = new AtomicReference<>();
        try (WasiCtx wasi = new WasiCtxBuilder().inheritStdout().inheritStderr().build();
             Store<Void> store = Store.withoutData(wasi);
             Linker linker = new Linker(store.engine());
             Module module = Module.fromBinary(store.engine(), data)) {

            this.wasi = wasi;
            this.store = store;
            this.linker = linker;
            this.module = module;

            WasiCtx.addToLinker(linker);
            try (Binding<Void> binding = bind(linker, store, memory)) {
                this.binding = binding;
                System.err.println("Instantiating module...");
                linker.module(store, "", module);
                // Next we poke around a bit to extract the `run` function from the module.
                System.err.println("Extracting export...");
                try (Memory mem = linker.get(store, "", "memory").orElseThrow().memory();
                     Func doWorkFn = linker.get(store, "", "vefi_main").orElseThrow().func()) {
                    this.mem = mem;
                    this.fn = doWorkFn;
                    memory.set(mem);
                    WasmFunctions.Consumer0 fn = WasmFunctions.consumer(store, doWorkFn);

                    // And last but not least we can call it!
                    System.err.println("Calling export...");
                    fn.accept();

                    System.err.println("Done.");
                }
            }
        } catch (Exception e) {
            LOGGER.error("E", e);
        }

        wasi = null;
        store = null;
        linker = null;
        module = null;
        binding = null;
        mem = null;
        fn = null;
    }

    public void close() {
        wasi.close();
        wasi = null;
        store.close();
        store = null;
        linker.close();
        linker = null;
        module.close();
        module = null;
        try {
            binding.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        binding = null;
        mem.close();
        mem = null;
        fn.close();
        fn = null;
    }

    public static class Binding<T> implements AutoCloseable {
        private final Store<T> store;
        private final Linker linker;
        private final AtomicReference<Memory> memory;
        private final List<AutoCloseable> closeables = new ArrayList<>();

        public Binding(Store<T> store, Linker linker, AtomicReference<Memory> memory) {
            this.store = store;
            this.linker = linker;
            this.memory = memory;
        }

        @Override
        public void close() throws Exception {
            Exception ex = null;
            for (AutoCloseable closeable : List.copyOf(closeables)) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    if (ex == null) ex = new Exception();
                    ex.addSuppressed(e);
                }
                closeables.remove(closeable);
            }

            if (ex != null) {
                throw ex;
            }
        }

        public void add(String moduleName, String name, Func func) {
            closeables.add(func);
            linker.define(store, moduleName, name, Extern.fromFunc(func));
        }

        public void add(String moduleName, String name, Memory memory) {
            closeables.add(memory);
            linker.define(store, moduleName, name, Extern.fromMemory(memory));
        }

        public void add(String moduleName, String name, Global global) {
            closeables.add(global);
            linker.define(store, moduleName, name, Extern.fromGlobal(global));
        }

        public void add(String moduleName, String name, Table table) {
            closeables.add(table);
            linker.define(store, moduleName, name, Extern.fromTable(table));
        }

        public void proxy(WasmProxy proxy) {
            Class<? extends WasmProxy> aClass = proxy.getClass();
            WasmModule mod = aClass.getAnnotation(WasmModule.class);
            if (mod == null) throw new IllegalArgumentException("WASM Proxy class is not annotated with @WasmModule");
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                WasmFunction annotation = method.getAnnotation(WasmFunction.class);
                if (annotation == null) continue;
                String name = annotation.name();
                List<Deserializer<?>> deserializers = new ArrayList<>();
                Val.Type[] params1 = paramsOf(method, deserializers);
                Val.Type[] returned = returnOf(method);
                Func func = new Func(
                        store,
                        new FuncType(params1, returned),
                        (caller, params, results) -> {
                            try {
                                List<Val> list = new ArrayList<>(List.of(params));
                                ByteBuffer buf = memory.get().buffer(store);
                                List<Object> args = new ArrayList<>();
                                for (Deserializer<?> deserializer : deserializers) {
                                    args.add(deserializer.pop(list, buf));
                                }

                                Object invoke = method.invoke(proxy, args.toArray());
                                if (returned.length == 1) {
                                    results[0] = switch (returned[0]) {
                                        case I32 -> Val.fromI32((int) invoke);
                                        case I64 -> Val.fromI64((long) invoke);
                                        case F32 -> Val.fromF32((float) invoke);
                                        case F64 -> Val.fromF64((double) invoke);
                                        case FUNC_REF -> new Val(Val.Type.FUNC_REF, invoke);
                                        default -> throw new IllegalStateException("Unexpected value: " + returned[0]);
                                    };
                                }
                            } catch (Exception e) {
                                LOGGER.error("Error when invoking VEFI firmware function:", e);
                            } catch (Throwable e) {
                                LOGGER.error("Fatal error when invoking VEFI firmware function:", e);
                            }
                        }
                );
                add(mod.name(), name, func);
            }

            proxy.load(memory::get, store);
        }

        private Val.Type[] paramsOf(Method method, List<Deserializer<?>> deserializers) {
            Parameter[] parameters = method.getParameters();
            List<Val.Type> types = new ArrayList<>();
            for (Parameter parameter : parameters) {
                Class<?> type = parameter.getType();
                addType(type, types);
                addDeserializer(type, deserializers);
            }

            return types.toArray(Val.Type[]::new);
        }

        private Val.Type[] returnOf(Method method) {
            Class<?> returnType = method.getReturnType();
            if (returnType == void.class) {
                return new Val.Type[0];
            }
            List<Val.Type> types = new ArrayList<>();
            addType(returnType, types);
            if (types.size() > 1) {
                throw new IllegalStateException("Can't return more than once");
            }

            return types.toArray(Val.Type[]::new);
        }

        public interface Deserializer<T> {
            Deserializer<Integer> I32 = (vals, mem) -> vals.removeFirst().i32();
            Deserializer<Long> I64 = (vals, mem) -> vals.removeFirst().i64();
            Deserializer<Float> F32 = (vals, mem) -> vals.removeFirst().f32();
            Deserializer<Double> F64 = (vals, mem) -> vals.removeFirst().f64();
            Deserializer<Func> FUNC_REF = (vals, mem) -> (Func) vals.removeFirst().getValue();
            Deserializer<String> STRING = (vals, mem) -> {
                long ptr = vals.removeFirst().i64();
                int len = vals.removeFirst().i32();

                return stringFromPtr(ptr, len, mem);
            };
            Deserializer<UUID> UUID = (vals, mem) -> {
                long msb = vals.removeFirst().i64();
                long lsb = vals.removeFirst().i64();

                return new UUID(msb, lsb);
            };

            T pop(List<Val> vals, ByteBuffer buf);
        }

        private static void addDeserializer(Class<?> type, List<Deserializer<?>> deserializers) {
            if (type == int.class) {
                deserializers.add(Deserializer.I32);
            } else if (type == long.class) {
                deserializers.add(Deserializer.I64);
            } else if (type == float.class) {
                deserializers.add(Deserializer.F32);
            } else if (type == double.class) {
                deserializers.add(Deserializer.F64);
            } else if (type == Func.class) {
                deserializers.add(Deserializer.FUNC_REF);
            } else if (type == String.class) {
                deserializers.add(Deserializer.STRING);
            } else {
                throw new IllegalStateException("Invalid type: " + type.getName());
            }
        }

        private static void addType(Class<?> type, List<Val.Type> types) {
            if (type == int.class) {
                types.add(Val.Type.I32);
            } else if (type == long.class) {
                types.add(Val.Type.I64);
            } else if (type == float.class) {
                types.add(Val.Type.F32);
            } else if (type == double.class) {
                types.add(Val.Type.F64);
            } else if (type == Func.class) {
                types.add(Val.Type.FUNC_REF);
            } else if (type == String.class) {
                types.add(Val.Type.I64);
                types.add(Val.Type.I32);
            } else {
                throw new IllegalStateException("Invalid type: " + type.getName());
            }
        }
    }

    public Binding<Void> bind(Linker linker, Store<Void> store, AtomicReference<Memory> memory) {
        Binding<Void> binding = new Binding<>(store, linker, memory);
        for (WasmProxy proxy : proxies)
            binding.proxy(proxy);
        return binding;
    }

    public Store<Void> getStore() {
        return store;
    }

    public Linker getLinker() {
        return linker;
    }

    public Module getModule() {
        return module;
    }

    public WasiCtx getWasi() {
        return wasi;
    }

    public Binding<Void> getBinding() {
        return binding;
    }

    public Memory getMem() {
        return mem;
    }

    public Func getFn() {
        return fn;
    }

    private static @Nullable String stringFromPtr(Long ptr, Integer len, ByteBuffer buf) {
        if (ptr >= Integer.MAX_VALUE) {
            System.err.printf("[WASM] invalid string pointer 0x%08x%n", ptr);
            throw new IndexOutOfBoundsException(ptr);
        }
        if (ptr < 0 || ptr >= buf.capacity()) {
            System.err.printf("[WASM] invalid string pointer 0x%08x%n", ptr);
            return null;
        }
        System.err.printf("Pointer 0x%08x%n", ptr);

        buf.position(Math.toIntExact(ptr - 1));
        buf.get();
        byte[] data = new byte[len];
        buf.get(data);

        return new String(data, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws IOException {
        try (InputStream stream = VEfiApplication.class.getResourceAsStream("/mineos.wasm")) {
            if (stream != null) {
                VEfiApplication VEfiApplication = new VEfiApplication(stream.readAllBytes(), "mineos.wasm", new VEfi());
                VEfiApplication.setDaemon(false);
                VEfiApplication.start();
            } else {
                throw new FileNotFoundException("/mineos.wasm");
            }
        }
    }
}