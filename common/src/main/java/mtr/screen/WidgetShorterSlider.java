package mtr.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.data.IGui;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Consumer;
import java.util.function.Function;

public class WidgetShorterSlider extends AbstractSliderButton implements IGui {

	private final int maxValue;
	private final Function<Integer, String> setMessage;
	private final Consumer<Integer> shiftClickAction;

	private static final int SLIDER_WIDTH = 6;

	public WidgetShorterSlider(int x, int width, int maxValue, Function<Integer, String> setMessage, Consumer<Integer> shiftClickAction) {
		super(x, 0, width, 0, new TextComponent(""), 0);
		this.maxValue = maxValue;
		this.setMessage = setMessage;
		this.shiftClickAction = shiftClickAction;
	}

	@Override
	public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
		final Minecraft client = Minecraft.getInstance();
		UtilitiesClient.beginDrawingTexture(WIDGETS_LOCATION);

		blit(matrices, x, y, 0, 46, width / 2, height / 2);
		blit(matrices, x, y + height / 2, 0, 66 - height / 2, width / 2, height / 2);
		blit(matrices, x + width / 2, y, 200 - width / 2, 46, width / 2, height / 2);
		blit(matrices, x + width / 2, y + height / 2, 200 - width / 2, 66 - height / 2, width / 2, height / 2);

		final int v = UtilitiesClient.isHovered(this) ? 86 : 66;
		final int xOffset = (width - SLIDER_WIDTH) * getIntValue() / maxValue;
		blit(matrices, x + xOffset, y, 0, v, SLIDER_WIDTH / 2, height / 2);
		blit(matrices, x + xOffset, y + height / 2, 0, v + 20 - height / 2, SLIDER_WIDTH / 2, height / 2);
		blit(matrices, x + xOffset + SLIDER_WIDTH / 2, y, 200 - SLIDER_WIDTH / 2, v, SLIDER_WIDTH / 2, height / 2);
		blit(matrices, x + xOffset + SLIDER_WIDTH / 2, y + height / 2, 200 - SLIDER_WIDTH / 2, v + 20 - height / 2, SLIDER_WIDTH / 2, height / 2);

		drawString(matrices, client.font, getMessage().getString(), x + width + TEXT_PADDING, y + (height - TEXT_HEIGHT) / 2, ARGB_WHITE);
	}

	@Override
	public void onClick(double d, double e) {
		super.onClick(d, e);
		checkShiftClick();
	}

	@Override
	protected void updateMessage() {
		setMessage(new TextComponent(setMessage.apply(getIntValue())));
	}

	@Override
	protected void onDrag(double d, double e, double f, double g) {
		super.onDrag(d, e, f, g);
		checkShiftClick();
	}

	@Override
	protected void applyValue() {
	}

	public void setValue(int valueInt) {
		value = (double) valueInt / maxValue;
		updateMessage();
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getIntValue() {
		return (int) Math.round(value * maxValue);
	}

	private void checkShiftClick() {
		if (shiftClickAction != null && Screen.hasShiftDown()) {
			shiftClickAction.accept(getIntValue());
		}
	}
}
