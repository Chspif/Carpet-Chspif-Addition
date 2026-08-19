package chspif;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SharedMailBox
{
    public static final int SLOT_COUNT = 54;
    public static final UUID FAKE_PLAYER_UUID = UUID.nameUUIDFromBytes("ChspifMailbox".getBytes(StandardCharsets.UTF_8));
    private static final String INVENTORY_TAG = "Inventory";

    private static SharedMailBox instance;

    private final SimpleContainer container = new SimpleContainer(SLOT_COUNT);
    private final List<MailMenu> viewers = new ArrayList<>();
    private MinecraftServer server;
    private boolean loaded;

    public static SharedMailBox getInstance()
    {
        if (instance == null)
        {
            instance = new SharedMailBox();
        }
        return instance;
    }

    public static void openFor(ServerPlayer player)
    {
        getInstance().open(player);
    }

    public void open(ServerPlayer player)
    {
        this.server = player.level().getServer();
        if (!loaded)
        {
            load();
            loaded = true;
        }
        player.openMenu(provider());
    }

    public SimpleContainer container()
    {
        return container;
    }

    public List<MailMenu> viewers()
    {
        return viewers;
    }

    public void addViewer(MailMenu menu)
    {
        viewers.add(menu);
    }

    public void removeViewer(MailMenu menu)
    {
        viewers.remove(menu);
    }

    public void markChanged()
    {
        save();
    }

    private MenuProvider provider()
    {
        return new SimpleMenuProvider((syncId, inventory, player) -> new MailMenu(syncId, inventory, (ServerPlayer) player, this),
                Component.literal("快捷邮寄"));
    }

    private Path playerDataPath()
    {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(FAKE_PLAYER_UUID + ".dat");
    }

    private void load()
    {
        Path path = playerDataPath();
        if (!Files.exists(path))
        {
            return;
        }
        try
        {
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            if (root.get(INVENTORY_TAG) instanceof ListTag list)
            {
                RegistryAccess registryAccess = server.registryAccess();
                for (int i = 0; i < list.size(); i++)
                {
                    CompoundTag tag = list.getCompound(i).orElse(null);
                    if (tag == null)
                    {
                        continue;
                    }
                    byte slotByte = tag.getByte("Slot").orElse((byte) 0);
                    int slot = slotByte & 0xFF;
                    if (slot >= 0 && slot < SLOT_COUNT)
                    {
                        ItemStack stack = ItemStack.CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), tag)
                                .result().orElse(ItemStack.EMPTY);
                        container.setItem(slot, stack);
                    }
                }
            }
        }
        catch (IOException e)
        {
        }
    }

    private void save()
    {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        RegistryAccess registryAccess = server.registryAccess();
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty())
            {
                Tag encoded = ItemStack.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack)
                        .result().orElse(null);
                if (encoded instanceof CompoundTag tag)
                {
                    tag.putByte("Slot", (byte) i);
                    list.add(tag);
                }
            }
        }
        root.put(INVENTORY_TAG, list);
        try
        {
            NbtIo.writeCompressed(root, playerDataPath());
        }
        catch (IOException e)
        {
        }
    }

    public void onServerClosed()
    {
        save();
        viewers.clear();
        instance = null;
    }
}
