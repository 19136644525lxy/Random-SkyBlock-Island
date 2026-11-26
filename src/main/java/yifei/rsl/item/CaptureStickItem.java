package yifei.rsl.item;

import net.minecraft.item.Item;

/**
 * "收纳木棍"：空手右键容器不会抓取；潜行 + 右键把带内容容器的方块状态与
 * 方块实体 NBT 加入正常方块池。
 */
public class CaptureStickItem extends Item {
	public CaptureStickItem(Settings settings) {
		super(settings);
	}
}
