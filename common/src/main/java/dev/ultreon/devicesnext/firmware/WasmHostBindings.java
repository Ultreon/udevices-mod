package dev.ultreon.devicesnext.firmware;

import io.github.kawamuray.wasmtime.*;
import io.github.kawamuray.wasmtime.Module;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WasmHostBindings {
    record UUID64(long msb, long lsb) {}

    record Drive(UUID64 uuid) {}

    record Graphics(int width, int height) {}

    record Logger(String name) {}

    record FileSystem() {}

    private final Map<Integer, Graphics> graphicsMap = new HashMap<>();
    private final Map<Integer, Logger> loggerMap = new HashMap<>();
    private final Map<Integer, Drive> driveMap = new HashMap<>();
    private final Map<Integer, FileSystem> fsMap = new HashMap<>();

    private final AtomicInteger nextId = new AtomicInteger(1);

    public void bind(Linker linker, Store<Void> store, Memory memory) {
        // Define 'env::print' with explicit (i32) -> void signature and pointer validation.
        FuncType printFuncType = new FuncType(new Val.Type[]{Val.Type.I32}, new Val.Type[]{});
        Func printFunc = new Func(store, printFuncType, (caller, params, results) -> {
            int ptr = params[0].i32();

            ByteBuffer buf = memory.buffer(store);
            if (ptr < 0 || ptr >= buf.capacity()) {
                System.err.println("[WASM] print: invalid pointer " + ptr);
                return;
            }

            buf.position(ptr);
            int b;
            int len = 0;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while ((b = buf.get()) != 0 && len < 65536) {
                bos.write(b);
                len++;
            }

            System.out.println("[WASM] " + bos.toString(StandardCharsets.UTF_8));
        });
        linker.define(store, "env", "print", Extern.fromFunc(printFunc));


//        // Implement others like debug, warn, error, create_image, dispose_image...
//        // You can use similar patterns as above.
//
//        // Example stub for open_main_drive and list_drives
//        linker.define(
//                store, "env", "open_main_drive", Extern.fromFunc(WasmFunctions.wrap(
//                        store, I32, () -> {
//                            int id = nextId.getAndIncrement();
//                            driveMap.put(id, new Drive(new UUID64(1234, 5678)));
//                            return id;
//                        }
//                ))
//        );
//
//        linker.define(
//                store, "env", "list_drives", Extern.fromFunc(WasmFunctions.wrap(
//                        store, I32, () -> {
//                            // Return fake pointer (or ID to a drive list)
//                            // You must use a custom structure if WASM expects memory-based results
//                            return 0; // Placeholder
//                        }
//                ))
//        );
    }

    public static void main(String[] args) {
        Config config = new Config();
        config.staticMemoryMaximumSize(2097152L);
        Engine engine = new Engine(config);
        try (Store<Void> store = Store.withoutData()) {
            Memory memory = new Memory(store, new MemoryType(0L, 65536, false));
            WasmHostBindings wasmHostBindings = new WasmHostBindings();
//            wasmHostBindings.bind(linker, store, memory);
//
            try (InputStream inputStream = WasmHostBindings.class.getResourceAsStream("/mineos.wasm")) {
                if (inputStream == null) throw new FileNotFoundException();
                Module module = Module.fromBinary(engine, inputStream.readAllBytes());
                // Create your print function (no linker.define)
                FuncType printFuncType = new FuncType(new Val.Type[]{Val.Type.I32}, new Val.Type[]{});
                Func printFunc = new Func(store, printFuncType, (caller, params, results) -> {
                    int ptr = params[0].i32();

                    ByteBuffer buf = memory.buffer(store);
                    if (ptr < 0 || ptr >= buf.capacity()) {
                        System.err.println("[WASM] print: invalid pointer " + ptr);
                        return;
                    }

                    buf.position(ptr);
                    int b;
                    int len = 0;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while ((b = buf.get()) != 0 && len < 65536) {
                        bos.write(b);
                        len++;
                    }

                    System.out.println("[WASM] " + bos.toString(StandardCharsets.UTF_8));
                });

                // Prepare import list matching exactly the module.imports order
                List<Extern> importList = new ArrayList<>();
                for (ImportType imp : module.imports()) {
                    if (imp.module().equals("env") && imp.name().equals("print")) {
                        importList.add(Extern.fromFunc(printFunc));
                    } else if (imp.module().equals("env") && imp.name().equals("memory")) {
                        importList.add(Extern.fromMemory(memory));
                    } else {
                        throw new IllegalStateException("Missing binding for: " + imp.module() + "::" + imp.name());
                    }
                }

                // Now instantiate passing imports directly (no linker.define)
                try (Instance instance = new Instance(store, module, List.of())) {
                    try (Func f = instance.getFunc(store, "vefi_main").orElseThrow()) {
                        WasmFunctions.Consumer0 fn = WasmFunctions.consumer(store, f);
                        fn.accept();
                    }
                }
//                ImportType[] imports = module.imports();
//                System.out.println("IMPORTS:");
//                for (ImportType imp : module.imports()) {
//                    if (imp.type() == ImportType.Type.FUNC) {
//                        FuncType funcType = imp.func();
//                        System.out.printf(
//                                " - %s::%s => FUNC %s -> %s%n",
//                                imp.module(), imp.name(),
//                                Arrays.toString(funcType.getParams()),
//                                Arrays.toString(funcType.getResults())
//                        );
//                    } else {
//                        System.out.printf(" - %s::%s => %s%n", imp.module(), imp.name(), imp.type());
//                    }
//                }
//                List<Extern> importList = new ArrayList<>();
//                // Add all bindings in the correct order (matching what module.imports() says)
//                for (ImportType imp : module.imports()) {
//                    if (imp.module().equals("env") && imp.name().equals("print")) {
//                        importList.add(linker.get(store, "env", "print").orElseThrow());
//                    } else {
//                        throw new IllegalStateException("Missing binding for: " + imp.module() + "::" + imp.name());
//                    }
//                }
//
//                linker.define(store, "env", "memory", Extern.fromMemory(memory));
//                try (Instance instance = new Instance(store, module, importList)) {
//                    try (Func f = instance.getFunc(store, "vefi_main").orElseThrow()) {
//                        WasmFunctions.Consumer0 fn = WasmFunctions.consumer(store, f);
//                        fn.accept();
//                    }
//                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
