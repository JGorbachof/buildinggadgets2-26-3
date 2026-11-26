package com.direwolf20.buildinggadgets2.common.network.handler;

import com.direwolf20.buildinggadgets2.common.containers.TemplateManagerContainer;
import com.direwolf20.buildinggadgets2.common.network.data.SendCopyDataPayload;
import com.direwolf20.buildinggadgets2.common.network.data.UpdateTemplateManagerPayload;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.datatypes.TagPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static com.direwolf20.buildinggadgets2.util.MiscHelpers.playSound;

public class PacketUpdateTemplateManager {
    public static final PacketUpdateTemplateManager INSTANCE = new PacketUpdateTemplateManager();

    public static PacketUpdateTemplateManager get() {
        return INSTANCE;
    }

    public void handle(final UpdateTemplateManagerPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            AbstractContainerMenu container = player.containerMenu;

            if (container == null || !(container instanceof TemplateManagerContainer))
                return;

            ItemStack gadgetStack = container.getSlot(0).getItem();
            ItemStack templateStack = container.getSlot(1).getItem();
            if (!container.stillValid(player)) return;
            if (payload.mode() == 0) { //Save
                if (templateStack.isEmpty()) { //Save the templateName to the Gadget if theres no paper in the slot
                    GadgetNBT.setTemplateName(gadgetStack, payload.templateName());
                    return;
                }

                if (templateStack.is(Items.PAPER)) {
                    UUID sourceUUID = GadgetNBT.getUUID(gadgetStack);
                    BG2Data bg2Data = BG2Data.get(Objects.requireNonNull(player.level().getServer()).overworld());
                    ArrayList<StatePos> buildList = bg2Data.getCopyPasteList(sourceUUID, false);
                    if (buildList == null || buildList.isEmpty()) {
                        playSound((ServerPlayer) player, Holder.direct(SoundEvents.WAXED_SIGN_INTERACT_FAIL));
                        return;
                    }
                    // Pre-assign the UUID on the template stack BEFORE placing it in the slot.
                    // In 26.1, setItem syncs the stack to the client. If the stack arrives without
                    // a GADGET_UUID, client-side getUUID() auto-assigns a random one, contaminating
                    // the client copy and causing UUID mismatches on load.
                    ItemStack newTemplate = new ItemStack(Registration.Template.get());
                    GadgetNBT.setUUID(newTemplate);
                    container.setItem(1, container.getStateId(), newTemplate);
                    templateStack = container.getSlot(1).getItem();
                }

                if (templateStack.is(Registration.Redprint.get())) {
                    if (payload.templateName().isEmpty()) {
                        player.sendOverlayMessage(Component.translatable("buildinggadgets2.messages.namerequired"));
                        playSound((ServerPlayer) player, Holder.direct(SoundEvents.WAXED_SIGN_INTERACT_FAIL));
                        return;
                    }
                }

                GadgetNBT.setTemplateName(templateStack, payload.templateName());

                if (gadgetStack.isEmpty())
                    return;

                copyData((ServerPlayer) player, gadgetStack, templateStack, payload.templateName());
                // Re-push both slots so the client receives all component mutations (UUID, COPY_UUID,
                // template name) that copyData applied in-place. In 26.1, in-place component changes
                // on container stacks aren't reliably detected by broadcastChanges.
                container.setItem(0, container.getStateId(), gadgetStack);
                container.setItem(1, container.getStateId(), templateStack);
            } else if (payload.mode() == 1) { //Load
                if (templateStack.isEmpty() || gadgetStack.isEmpty()) {
                    playSound((ServerPlayer) player, Holder.direct(SoundEvents.WAXED_SIGN_INTERACT_FAIL));
                    return;
                }
                if (templateStack.is(Registration.Redprint.get()) && !gadgetStack.is(Registration.CutPaste_Gadget.get())) {
                    playSound((ServerPlayer) player, Holder.direct(SoundEvents.WAXED_SIGN_INTERACT_FAIL));
                    return; //Redprints can only go onto Cut and Paste gadgets
                }
                if (gadgetStack.is(Registration.CutPaste_Gadget.get()) && !templateStack.is(Registration.Redprint.get())) {
                    playSound((ServerPlayer) player, Holder.direct(SoundEvents.WAXED_SIGN_INTERACT_FAIL));
                    return; //Cut and Paste gadgets can only be loaded from Redprints
                }

                copyData((ServerPlayer) player, templateStack, gadgetStack, payload.templateName());
                GadgetNBT.setTemplateName(gadgetStack, GadgetNBT.getTemplateName(templateStack)); //Set gadget template name to templatestack name
                container.setItem(0, container.getStateId(), gadgetStack);
                container.setItem(1, container.getStateId(), templateStack);
            }
        });
    }

    public static void copyData(ServerPlayer sender, ItemStack sourceStack, ItemStack targetStack, String templateName) {
        UUID targetUUID = GadgetNBT.getUUID(targetStack);
        UUID sourceUUID = GadgetNBT.getUUID(sourceStack);
        BG2Data bg2Data = BG2Data.get(Objects.requireNonNull(sender.level().getServer()).overworld());
        boolean moving = sourceStack.is(Registration.Redprint.get()) || targetStack.is(Registration.Redprint.get());
        if (moving) {
            boolean saving = sourceStack.is(Registration.CutPaste_Gadget.get()) && targetStack.is(Registration.Redprint.get());
            boolean loading = sourceStack.is(Registration.Redprint.get()) && targetStack.is(Registration.CutPaste_Gadget.get());
            if ((!saving && !loading) || com.direwolf20.buildinggadgets2.common.events.ServerTickHandler.gadgetWorking(sourceUUID)
                    || com.direwolf20.buildinggadgets2.common.events.ServerTickHandler.gadgetWorking(targetUUID)) return;
            if (saving && (templateName.isBlank() || bg2Data.getRedprintUUIDfromName(templateName) != null
                    || bg2Data.getRedprintLookup().containsKey(targetUUID))) return;
            if (!bg2Data.moveCutData(sourceUUID,targetUUID)) {
                playSound(sender, Holder.direct(SoundEvents.WAXED_SIGN_INTERACT_FAIL));
                return;
            }
            if (saving) {
                bg2Data.addToRedprints(targetUUID,templateName);
                GadgetNBT.clearCopyUUID(sourceStack);
                GadgetNBT.setMode(sourceStack,new com.direwolf20.buildinggadgets2.util.modes.Cut());
            }
            if (loading) {
                GadgetNBT.setMode(targetStack,new com.direwolf20.buildinggadgets2.util.modes.Paste());
            }
        } else {
            if (sourceStack.is(Registration.CutPaste_Gadget.get()) || targetStack.is(Registration.CutPaste_Gadget.get())) return;
            ArrayList<StatePos> buildList = bg2Data.getCopyPasteList(sourceUUID, false);
            if (buildList == null || buildList.isEmpty()) {
                playSound(sender, Holder.direct(SoundEvents.WAXED_SIGN_INTERACT_FAIL));
                return;
            }
            bg2Data.addToCopyPaste(targetUUID,new ArrayList<>(buildList));
        }
        GadgetNBT.setCopyUUID(targetStack);

        //Ensure client has the updated values for both objects
        CompoundTag tag = bg2Data.getCopyPasteListAsNBTMap(sourceUUID, false);
        if (sender.connection.hasChannel(SendCopyDataPayload.TYPE)) sender.connection.send(new SendCopyDataPayload(sourceUUID, GadgetNBT.getCopyUUID(sourceStack), tag));

        tag = bg2Data.getCopyPasteListAsNBTMap(targetUUID, false);
        if (sender.connection.hasChannel(SendCopyDataPayload.TYPE)) sender.connection.send(new SendCopyDataPayload(targetUUID, GadgetNBT.getCopyUUID(targetStack), tag));
        if (sourceStack.is(Registration.Redprint.get())) sourceStack.shrink(1);
        else if (moving) GadgetNBT.clearCopyUUID(sourceStack);

        playSound(sender, Holder.direct(SoundEvents.ENCHANTMENT_TABLE_USE));
    }
}
