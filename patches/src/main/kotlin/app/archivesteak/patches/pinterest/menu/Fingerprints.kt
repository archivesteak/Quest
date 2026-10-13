package app.archivesteak.patches.pinterest.menu

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/** Builds the long-press menu of a Pin. */
internal object PinLongPressMenuBuilderFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/pinterest/ui/menu/ContextMenuView;", "L", "Ljava/lang/String;"),
    filters = listOf(string("android_see_more_see_less_long_press")),
)

/** Shows the item views of a long-press menu. */
internal object ContextMenuSetItemsFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/ui/menu/ContextMenuView;",
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
)

/** Makes a long-press menu item view from an icon, label, description and action. */
internal object ContextMenuItemFactoryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Lcom/pinterest/ui/menu/ContextMenuItemView;",
    parameters = listOf("Landroid/content/Context;", "L"),
    filters = listOf(methodCall(name = "setOnClickListener")),
)
