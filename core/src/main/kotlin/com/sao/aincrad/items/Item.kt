package com.sao.aincrad.items

enum class ItemType {
    CURRENCY, WEAPON, ARMOR, CONSUMABLE, KEY_ITEM
}

data class Item(
    val id: String,
    val name: String,
    val type: ItemType,
    val description: String = "",
    val slot: EquipmentSlot? = null,
    val hpBonus: Int = 0,
    val mpBonus: Int = 0,
    val strBonus: Int = 0,
    val agiBonus: Int = 0,
    val intBonus: Int = 0,
    val defenseBonus: Int = 0,
)

data class ItemStack(
    val item: Item,
    var quantity: Int = 1,
)

enum class EquipmentSlot {
    SWORD, ARMOR, ACCESSORY
}

object Items {
    val col = Item("col", "Col", ItemType.CURRENCY)
    val hpPotion = Item("hp_potion", "HP Potion", ItemType.CONSUMABLE, "Restores HP.")
    val boarHide = Item("boar_hide", "Boar Hide", ItemType.CONSUMABLE, "A common crafting material.")
    val floorKeyShard = Item("floor_key_shard", "Floor Key Shard", ItemType.KEY_ITEM, "A fragment used to open the next gate.")
    val annealBlade = Item("anneal_blade", "Anneal Blade", ItemType.WEAPON, "Starter one-hand sword.", EquipmentSlot.SWORD, strBonus = 4)
    val darkRepulser = Item("dark_repulser", "Dark Repulser", ItemType.WEAPON, "High-level sword forged for boss floors.", EquipmentSlot.SWORD, strBonus = 10, agiBonus = 2)
    val blackwyrmCoat = Item("blackwyrm_coat", "Blackwyrm Coat", ItemType.ARMOR, "Light coat with strong mitigation.", EquipmentSlot.ARMOR, hpBonus = 30, agiBonus = 2, defenseBonus = 5)
    val teleportCrystal = Item("teleport_crystal", "Teleport Crystal", ItemType.KEY_ITEM, "Emergency escape crystal.", EquipmentSlot.ACCESSORY, mpBonus = 10, intBonus = 2)

    val byId = listOf(
        col,
        hpPotion,
        boarHide,
        floorKeyShard,
        annealBlade,
        darkRepulser,
        blackwyrmCoat,
        teleportCrystal,
    ).associateBy { it.id }
}
