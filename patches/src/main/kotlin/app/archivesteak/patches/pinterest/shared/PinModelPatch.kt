package app.archivesteak.patches.pinterest.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.ClassDef

private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/shared/PinModel;"

internal lateinit var pinClass: ClassDef
    private set

/**
 * Implements the extension's accessors for the obfuscated Pin model,
 * so extension code can read Pins without knowing their class or getter names.
 *
 * The compiler may give an extension stub no registers besides its parameters,
 * so implementations only use parameter registers.
 */
val pinModelPatch = bytecodePatch {
    extendWith("extensions/pinterest.mpe")

    execute {
        pinClass = modelClassWithJsonFields("pinner", "images", "videos", "created_at")
        val createdAtGetter = pinClass.getterOf(pinClass.fieldWithJsonName("created_at"))

        val imagesField = pinClass.fieldWithJsonName("images")
        val imagesGetter = pinClass.getterOf(imagesField)
        val imageClass = classDefBy(imagesField.mapValueType)
        val imageUrlGetter = imageClass.getterOf(imageClass.fieldWithJsonName("url"))

        val videosGetter = pinClass.getterOf(pinClass.fieldWithJsonName("videos"))
        val videoClass = classDefBy(videosGetter.returnType)
        val videoListField = videoClass.fieldWithJsonName("video_list")
        val videoListGetter = videoClass.getterOf(videoListField)
        val videoEncodingClass = classDefBy(videoListField.mapValueType)
        val videoUrlGetter = videoEncodingClass.getterOf(videoEncodingClass.fieldWithJsonName("url"))

        val extensionMethods = mutableClassDefBy(EXTENSION_CLASS).methods
        fun implement(name: String, smali: String) =
            extensionMethods.single { it.name == name }.addInstructions(0, smali)

        implement(
            "getCreatedAt",
            """
                check-cast p0, ${pinClass.type}
                invoke-virtual { p0 }, ${createdAtGetter.smaliReference}
                move-result-object p0
                return-object p0
            """
        )
        implement(
            "getImages",
            """
                check-cast p0, ${pinClass.type}
                invoke-virtual { p0 }, ${imagesGetter.smaliReference}
                move-result-object p0
                return-object p0
            """
        )
        implement(
            "getImageUrl",
            """
                check-cast p0, ${imageClass.type}
                invoke-virtual { p0 }, ${imageUrlGetter.smaliReference}
                move-result-object p0
                return-object p0
            """
        )
        implement(
            "getVideoList",
            """
                check-cast p0, ${pinClass.type}
                invoke-virtual { p0 }, ${videosGetter.smaliReference}
                move-result-object p0
                if-eqz p0, :no_video
                invoke-virtual { p0 }, ${videoListGetter.smaliReference}
                move-result-object p0
                :no_video
                return-object p0
            """
        )
        implement(
            "getVideoUrl",
            """
                check-cast p0, ${videoEncodingClass.type}
                invoke-virtual { p0 }, ${videoUrlGetter.smaliReference}
                move-result-object p0
                return-object p0
            """
        )
    }
}
