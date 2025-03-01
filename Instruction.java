
public class Instruction {
    public enum State { IF, ID, IS, EX, WB };
    private static int globalCounter = 0;
    
    private State state;
    private int tag;

    private String PC;
    private int optype;
    private int dest;
    private int src1;
    private int src2;
    private int remainingCycles;
    private boolean src1Ready = false;
    private boolean src2Ready = false;

    public Instruction(String PC, int optype, int dest, int src1, int src2) {
        this.state = State.IF;
        this.tag = globalCounter++;
        this.PC = PC;
        this.optype = optype;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
        this.remainingCycles = getLatency();
    }

    public int getTag() { return tag; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public int getRemainingCycles() { return remainingCycles; }
    public void decrementCycle() { remainingCycles--; }
    public boolean isExecutionComplete() { return remainingCycles == 0; }
    public boolean isReady() { return src1Ready && src2Ready; }
    public void setSrc1Ready() { src1Ready = true; }
    public void setSrc2Ready() { src2Ready = true; }
    public int getDest() { return dest; }
    public int getSrc1() { return src1; }
    public int getSrc2() { return src2; }

    private int getLatency() {
        return switch (optype) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 5;
            default -> 1; // Default latency
        };
    }

    public void advanceState() {
        switch (state) {
            case IF -> state = State.ID;
            case ID -> state = State.IS;
            case IS -> state = State.EX;
            case EX -> state = State.WB;
            case WB -> {} // Do nothing - final state
        }
    }

}
