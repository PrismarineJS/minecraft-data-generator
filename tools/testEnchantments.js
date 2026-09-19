const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

// Pass the generated version directory; no snapshots or generated fields are rewritten.
const directory = process.argv[2]
assert.ok(directory, 'Usage: node tools/testEnchantments.js <generated-version-directory>')
const enchantments = JSON.parse(fs.readFileSync(path.join(directory, 'enchantments.json')))
const items = JSON.parse(fs.readFileSync(path.join(directory, 'items.json')))
const names = new Set(items.map(item => item.name))
const byName = Object.fromEntries(enchantments.map(enchantment => [enchantment.name, enchantment]))
for (const enchantment of enchantments) {
  assert.ok(Array.isArray(enchantment.supportedItems), enchantment.name)
  assert.equal(new Set(enchantment.supportedItems).size, enchantment.supportedItems.length)
  assert.ok(['common', 'uncommon', 'rare', 'very_rare'].includes(enchantment.rarity))
  for (const item of enchantment.supportedItems) assert.ok(names.has(item), item)
}
for (const [enchantment, item, accepted] of [
  ['sharpness', 'diamond_axe', true],
  ['knockback', 'diamond_axe', false],
  ['fire_aspect', 'diamond_axe', false],
  ['thorns', 'diamond_helmet', true],
  ['sharpness', 'diamond_sword', true],
  ['sharpness', 'stone', false]
]) {
  assert.equal(byName[enchantment].supportedItems.includes(item), accepted, `${enchantment}/${item}`)
}
assert.equal(byName.sharpness.rarity, 'common')
assert.equal(byName.thorns.rarity, 'very_rare')
console.log(`Validated ${enchantments.length} enchantments and native applicability regression cases`)
