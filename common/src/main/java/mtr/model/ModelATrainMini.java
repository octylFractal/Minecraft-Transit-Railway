package mtr.model;

public class ModelATrainMini extends ModelATrain {

	public ModelATrainMini(boolean isAel) {
		super(isAel);
	}

	@Override
	protected int[] getWindowPositions() {
		return isAel ? new int[]{-93, -67, -41, 41, 67, 93} : new int[]{0};
	}

	@Override
	protected int[] getDoorPositions() {
		return isAel ? new int[]{0} : new int[]{-40, 40};
	}

	@Override
	protected int[] getEndPositions() {
		return isAel ? new int[]{-104, 104} : new int[]{-64, 64};
	}

	@Override
	protected int[] getBogiePositions() {
		return new int[]{0};
	}
}