package thearchis.common.card;

public enum CardRarity {
    COMMON(40, 5),
    RARE(100, 20),
    EPIC(400, 100),
    LEGENDARY(1600, 400);

    private final int craftCost;
    private final int disenchantValue;

    CardRarity(int craftCost, int disenchantValue) {
        this.craftCost = craftCost;
        this.disenchantValue = disenchantValue;
    }

    public int getCraftCost() {
        return craftCost;
    }

    public int getDisenchantValue() {
        return disenchantValue;
    }
}
