package com.direwolf20.buildinggadgets2.datagen;

import com.direwolf20.buildinggadgets2.BuildingGadgets2;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.minecraft.core.registries.Registries;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = BuildingGadgets2.MODID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(true, new BG2ModelProvider(packOutput));
        generator.addProvider(true, new BG2LanguageProvider(packOutput, "en_us"));

        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getWorldLookupProvider();

        generator.addProvider(true, new BG2BlockTags(packOutput, lookupProvider));
        generator.addProvider(true, new BG2ItemTags(packOutput, lookupProvider));
    }
    @SubscribeEvent
    public static void gatherRegistries(GatherDataRegistryEntriesEvent event) {
        event.add(BG2Recipes.bootstrap());
        event.add(Registries.LOOT_TABLE, new LootTableProvider(
                Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(BG2LootTables::new,LootContextParamSets.BLOCK))));
    }
}
