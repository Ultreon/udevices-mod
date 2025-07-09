package dev.ultreon.devicesnext.filesystem;

import org.jnode.driver.Device;
import org.jnode.partitions.gpt.GptPartitionTable;
import org.jnode.partitions.gpt.GptPartitionTableType;

public class GPT {
    public static final GptPartitionTable INSTANCE = new GptPartitionTable(new GptPartitionTableType(false, -1), -1, new byte[16384], new Device());
}
