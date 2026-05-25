package com.sao.aincrad.items

class Inventory(
    val columns: Int = 4,
    val rows: Int = 6,
) {
    private val slots = MutableList<ItemStack?>(columns * rows) { null }
    private val equipped = mutableMapOf<EquipmentSlot, Item>()

    val capacity: Int
        get() = slots.size

    fun allSlots(): List<ItemStack?> = slots

    fun add(item: Item, quantity: Int = 1): Boolean {
        if (quantity <= 0) return true

        val existingIndex = slots.indexOfFirst { it?.item?.id == item.id }
        if (existingIndex >= 0) {
            slots[existingIndex]?.quantity = (slots[existingIndex]?.quantity ?: 0) + quantity
            return true
        }

        val emptyIndex = slots.indexOfFirst { it == null }
        if (emptyIndex < 0) return false

        slots[emptyIndex] = ItemStack(item, quantity)
        return true
    }

    fun count(itemId: String): Int {
        return slots.firstOrNull { it?.item?.id == itemId }?.quantity ?: 0
    }

    fun equipped(slot: EquipmentSlot): Item? = equipped[slot]

    fun equippedItems(): Map<EquipmentSlot, Item> = equipped.toMap()

    fun equipFromSlot(index: Int): Boolean {
        val stack = slots.getOrNull(index) ?: return false
        val slot = stack.item.slot ?: return false
        equipped[slot] = stack.item
        return true
    }

    fun equip(itemId: String): Boolean {
        val stackIndex = slots.indexOfFirst { it?.item?.id == itemId }
        return equipFromSlot(stackIndex)
    }

    fun removeAt(index: Int) {
        if (index in slots.indices) slots[index] = null
    }

    fun serialize(): String {
        val slotData = slots.map { stack ->
            if (stack == null) "" else "${stack.item.id}:${stack.quantity}"
        }.joinToString("|")
        val equipData = equipped.entries.joinToString("|") { "${it.key.name}:${it.value.id}" }
        return "$slotData#$equipData"
    }

    fun load(serialized: String) {
        slots.indices.forEach { slots[it] = null }
        equipped.clear()
        if (serialized.isBlank()) return

        val parts = serialized.split("#", limit = 2)
        parts.getOrNull(0)
            ?.split("|")
            ?.forEachIndexed { index, raw ->
                if (raw.isBlank() || index !in slots.indices) return@forEachIndexed
                val itemParts = raw.split(":", limit = 2)
                val item = Items.byId[itemParts.getOrNull(0)] ?: return@forEachIndexed
                val quantity = itemParts.getOrNull(1)?.toIntOrNull() ?: 1
                slots[index] = ItemStack(item, quantity)
            }

        parts.getOrNull(1)
            ?.split("|")
            ?.forEach { raw ->
                if (raw.isBlank()) return@forEach
                val equipParts = raw.split(":", limit = 2)
                val slot = equipParts.getOrNull(0)?.let { runCatching { EquipmentSlot.valueOf(it) }.getOrNull() } ?: return@forEach
                val item = Items.byId[equipParts.getOrNull(1)] ?: return@forEach
                equipped[slot] = item
            }
    }

    fun statBonuses(): EquipmentStats {
        val items = equipped.values
        return EquipmentStats(
            hp = items.sumOf { it.hpBonus },
            mp = items.sumOf { it.mpBonus },
            str = items.sumOf { it.strBonus },
            agi = items.sumOf { it.agiBonus },
            intel = items.sumOf { it.intBonus },
            defense = items.sumOf { it.defenseBonus },
        )
    }
}

data class EquipmentStats(
    val hp: Int = 0,
    val mp: Int = 0,
    val str: Int = 0,
    val agi: Int = 0,
    val intel: Int = 0,
    val defense: Int = 0,
)
