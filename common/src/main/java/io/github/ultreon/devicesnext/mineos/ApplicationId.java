package com.ultreon.mods.lib.client.devicetest;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ApplicationId implements Comparable<ApplicationId> {
    private final String group;
    private final String module;

    public ApplicationId(String group, String module) {
        if (!checkGroup(group)) throw new IllegalArgumentException("Invalid group id: " + group);
        if (!checkModule(module)) throw new IllegalArgumentException("Invalid module name: " + module);
        this.group = group;
        this.module = module;
    }

    public ApplicationId(String id) {
        this(resolve(id));
    }

    private ApplicationId(String[] resolved) {
        this(resolved[0], resolved[1]);
    }

    private static String[] resolve(String id) {
        String[] idArray = id.split(":");
        if (idArray.length != 2)
            throw new IllegalArgumentException("Invalid application id, requires to contain 1 colon: " + id);

        return idArray;
    }

    private static boolean checkGroup(String group) {
        return group.matches("[a-zA-Z\\-]+(\\.[a-zA-Z\\-]+)*");
    }

    private static boolean checkModule(String module) {
        return module.matches("[a-zA-Z\\-]+");
    }

    public String getGroup() {
        return group;
    }

    public String getModule() {
        return module;
    }

    public static ApplicationId tryParse(String id) {
        String[] resolve = resolve(id);
        String group = resolve[0];
        String module = resolve[1];
        if (!checkGroup(group)) return null;
        if (!checkModule(module)) return null;

        return new ApplicationId(group, module);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationId that = (ApplicationId) o;
        return Objects.equals(getGroup(), that.getGroup()) && Objects.equals(getModule(), that.getModule());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroup(), getModule());
    }

    @Override
    public String toString() {
        return this.group + ":" + this.module;
    }

    public Component getName() {
        return Component.translatable("ultreonlib.application." + this.toString());
    }

    @Override
    public int compareTo(@NotNull ApplicationId other) {
        int group = this.group.compareTo(other.group);

        if (group == 0) {
            return this.module.compareTo(other.module);
        }

        return group;
    }
}
