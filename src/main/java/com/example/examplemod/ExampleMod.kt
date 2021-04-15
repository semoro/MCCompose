package com.example.examplemod

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.Logger
import org.lwjgl.input.Keyboard
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent

import net.minecraftforge.fml.common.eventhandler.EventPriority

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import net.minecraftforge.fml.relauncher.SideOnly
import sun.misc.Signal
import sun.misc.SignalHandler


@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION)
@EventBusSubscriber(Side.CLIENT)
class ExampleMod {
    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog

//        ClientRegistry.registerKeyBinding(keyBinding)
    }

    @Mod.EventHandler
//    @SideOnly(Side.CLIENT)
    fun init(event: FMLInitializationEvent?) {
        // some example code
//        logger!!.info("DIRT BLOCK >> {}", Blocks.DIRT.registryName)

        Signal.handle(Signal("TERM")) {
            println("Received SIGTERM")
        }

    }

    //    @SubscribeEvent

    //    public static void onMainMenu(GuiOpenEvent event) {
    //        if (event.getGui() instanceof GuiMainMenu) {
    //            MyGui g = new MyGui();
    //            event.setGui(g);
    //            Minecraft.getMinecraft().displayGuiScreen(g);
    //        }
    //    }
//    @SideOnly(Side.CLIENT)
//    val keyBinding = KeyBinding("Test compose gui", Keyboard.KEY_U, "examplemod.mc")

//    @SideOnly(Side.CLIENT)


    companion object {
        const val MODID = "examplemod"
        const val NAME = "Example Mod"
        const val VERSION = "1.0"
        private var logger: Logger? = null

    }
}