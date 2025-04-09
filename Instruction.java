public class Instruction {
    public enum State { IF, ID, IS, EX, WB };
    private static int globalCounter = 0;
    private State state;
    private String PC;
    private int tag;
    private int optype;
    private int dest;
    private int src1;
    private int src2;
    private int origsrc1;
    private int origsrc2;
    private int remainingCycles;
    private boolean src1Ready = false;
    private boolean src2Ready = false;
    private boolean isSrc1Tagged;
    private boolean isSrc2Tagged;

    private int IFCycle;
    private int IFTime;
    private int IDCycle;
    private int IDTime;
    private int ISCycle;
    private int ISTime;
    private int EXCycle;
    private int EXTime;
    private int WBCycle;
    private int WBTime;

    public Instruction(String PC, int optype, int dest, int src1, int src2) {
        this.state = State.IF;
        this.tag = globalCounter++;
        this.PC = PC;
        this.optype = optype;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
        this.origsrc1 = src1;
        this.origsrc2 = src2;
        this.isSrc1Tagged = false;
        this.isSrc2Tagged = false;
        this.remainingCycles = getLatency();
        this.IFCycle = -1;
        this.IFTime = 0;
        this.IDCycle = -1;
        this.IDTime = 0;
        this.ISCycle = -1;
        this.ISTime = 0;
        this.EXCycle = -1;
        this.EXTime = 0;
        this.WBCycle = -1;
        this.WBTime = 0;
    }

    // --- NEW METHOD FOR WATTAGE ---
    public int getWattage() {
        // Adjust these as needed
        return switch (optype) {
            case 0 -> 1;  // Wattage for op code 0
            case 1 -> 2;  // Wattage for op code 1
            case 2 -> 4;  // Wattage for op code 2
            default -> 0; // fallback, though we only have 0-2
        };
    }

    // You already have getOpType(), etc. Shown here for clarity.
    public int getOpType() { return optype; }
    public String getPC() { return PC; }
    public int getTag() { return tag; }
    public State getState() { return state; }
    public int getRemainingCycles() { return remainingCycles; }
    public int getDest() { return dest; }
    public int getSrc1() { return src1; }
    public int getSrc2() { return src2; }
    public int getOrigSrc1() { return origsrc1; }
    public int getOrigSrc2() { return origsrc2; }
    public boolean getIsSrc1Tagged() { return isSrc1Tagged; }
    public boolean getIsSrc2Tagged() { return isSrc2Tagged; }
    public int getIFCycle() { return IFCycle; }
    public int getIFTime() { return IFTime; }
    public int getIDCycle() { return IDCycle; }
    public int getIDTime() { return IDTime; }
    public int getISCycle() { return ISCycle; }
    public int getISTime() { return ISTime; }
    public int getEXCycle() { return EXCycle; }
    public int getEXTime() { return EXTime; }
    public int getWBCycle() { return WBCycle; }
    public int getWBTime() { return WBTime; }

    private int getLatency() {
        return switch (optype) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 5;
            default -> throw new IllegalArgumentException("optype invalid");
        };
    }

    public void setState(State state) { this.state = state; }
    public void setSrc1(int src1) { this.src1 = src1; }
    public void setSrc2(int src2) { this.src2 = src2; }
    public void setSrc1Ready() { src1Ready = true; }
    public void setSrc2Ready() { src2Ready = true; }
    public void setSrc1NotReady() { src1Ready = false; }
    public void setSrc2NotReady() { src2Ready = false; }
    public void setIsSrc1Tagged(boolean bool) { isSrc1Tagged = bool; }
    public void setIsSrc2Tagged(boolean bool) { isSrc2Tagged = bool; }

    public void updateTiming(int cycle) {
        switch (state) {
            case IF:
                if (IFCycle == -1) { IFCycle = cycle; }
                IFTime++;
                break;
            case ID:
                if (IDCycle == -1) { IDCycle = cycle; }
                IDTime++;
                break;
            case IS:
                if (ISCycle == -1) { ISCycle = cycle; }
                ISTime++;
                break;
            case EX:
                if (EXCycle == -1) { EXCycle = cycle; }
                EXTime++;
                break;
            case WB:
                if (WBCycle == -1) { WBCycle = cycle; }
                WBTime = 1;
                break;
            default:
                throw new IllegalArgumentException("Invalid state");
        }
    }

    public void decrementCycle() {
        remainingCycles--;
    }

    public boolean isExecutionComplete() {
        return remainingCycles == 0;
    }

    public boolean isReady() {
        return (src1 == -1 || src1Ready) && (src2 == -1 || src2Ready);
    }

    public void advanceState() {
        switch (state) {
            case IF -> state = State.ID;
            case ID -> state = State.IS;
            case IS -> state = State.EX;
            case EX -> state = State.WB;
            case WB -> {}
        }
    }
}
