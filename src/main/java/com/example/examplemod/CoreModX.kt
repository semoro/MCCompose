package com.example.examplemod

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin

@IFMLLoadingPlugin.TransformerExclusions("kotlin")
class CoreModX : IFMLLoadingPlugin {
    override fun getASMTransformerClass(): Array<String> {
        return arrayOf("com.example.examplemod.Transform")
    }

    override fun getModContainerClass(): String? {
        return null
    }

    override fun getSetupClass(): String? {
        return null
    }

    override fun injectData(p0: MutableMap<String, Any>?) {}

    override fun getAccessTransformerClass(): String? {
        return null
    }
}