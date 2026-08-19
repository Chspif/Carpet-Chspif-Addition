package chspif;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class MailMenu extends ChestMenu
{
    private final SharedMailBox mailbox;
    private final ServerPlayer owner;
    private final ItemStack[] lastShared = new ItemStack[SharedMailBox.SLOT_COUNT];

    public MailMenu(int syncId, Inventory playerInventory, ServerPlayer owner, SharedMailBox mailbox)
    {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, mailbox.container(), 6);
        this.owner = owner;
        this.mailbox = mailbox;
        mailbox.addViewer(this);
        SimpleContainer shared = mailbox.container();
        for (int i = 0; i < lastShared.length; i++)
        {
            lastShared[i] = shared.getItem(i).copy();
        }
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();
        SimpleContainer shared = mailbox.container();
        boolean changed = false;
        for (int i = 0; i < lastShared.length; i++)
        {
            ItemStack current = shared.getItem(i);
            if (notSame(current, lastShared[i]))
            {
                changed = true;
                for (MailMenu viewer : mailbox.viewers())
                {
                    if (viewer != this)
                    {
                        viewer.sendSlotChange(i, current);
                    }
                    viewer.lastShared[i] = current.copy();
                }
            }
        }
        if (changed)
        {
            mailbox.markChanged();
        }
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        mailbox.removeViewer(this);
    }

    private void sendSlotChange(int slot, ItemStack stack)
    {
        owner.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), slot, stack));
    }

    private static boolean notSame(ItemStack a, ItemStack b)
    {
        if (a.getCount() != b.getCount())
        {
            return true;
        }
        if (a.isEmpty() && b.isEmpty())
        {
            return false;
        }
        return !ItemStack.isSameItemSameComponents(a, b);
    }
}
