import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;

public class PipelineSimulator {
    // Helper method that constructs an Instruction from the trace file
    private Instruction createInstruction(Scanner s) {
        String PC    = s.next();
        int optype   = s.nextInt();
        int dest     = s.nextInt();
        int src1     = s.nextInt();
        int src2     = s.nextInt();
        return new Instruction(PC, optype, dest, src1, src2);
    }

    public PipelineSimulator(String[] args) throws IOException {
        // Expecting 5 arguments, e.g.:
        //   java sim S N checkN threshold traceFile
        int S           = Integer.parseInt(args[0]); // scheduling queue capacity
        int N           = Integer.parseInt(args[1]); // number of instructions to fetch each cycle
        int checkN      = Integer.parseInt(args[2]); // how many to sample before checking wattage
        int threshold   = Integer.parseInt(args[3]); // wattage threshold
        String traceFile= args[4];
        int total_wattage = 0;

        // Original pipeline variables
        int tag = 0;
        int cycle = 0;
        int maxDispatchSize = 2 * N;  // from your original code
        int schedulingCount = 0;      // how many in the scheduling queue
        int dispatchCount = 0;        // how many in the dispatch queue

        // New counters for your “n instructions in a row”
        int wattageSum   = 0;         // running sum of wattage for the last 'checkN' instructions
        int instrCount   = 0;         // how many instructions we've seen since last insert of NOPs

        // Pipeline data structures
        int[] RFTags = new int[128];
        FakeROB fakeROB = new FakeROB();
        RegisterFile rFile = new RegisterFile();
        List<Instruction> dispatchList = new ArrayList<>(); // Dispatch Queue
        List<Instruction> issueList    = new ArrayList<>(S); // Scheduling Queue
        List<Instruction> executeList  = new ArrayList<>(N + 1); // Functional Units
        List<Instruction> removalList  = new ArrayList<>(); // for collecting instructions to remove

        Scanner s = new Scanner(new File(traceFile));

        do {
            // -------------------
            // 1) FakeRetire()
            // -------------------
            removalList.clear();
            for (Instruction entry : fakeROB.entries) {
                if (entry.getState() == Instruction.State.WB) {
                    System.out.println(
                        entry.getTag() + " fu{" + entry.getOpType() + "} src{" + entry.getOrigSrc1()
                        + "," + entry.getOrigSrc2() + "} dst{" + entry.getDest() + "} IF{" + entry.getIFCycle()
                        + "," + entry.getIFTime() + "} ID{" + entry.getIDCycle() + "," + entry.getIDTime()
                        + "} IS{" + entry.getISCycle() + "," + entry.getISTime() + "} EX{" + entry.getEXCycle()
                        + "," + entry.getEXTime() + "} WB{" + entry.getWBCycle() + "," + entry.getWBTime() + "}"
                    );
                    removalList.add(entry);
                } else {
                    break;
                }
            }
            fakeROB.entries.removeAll(removalList);

            // -------------------
            // 2) Execute()
            // -------------------
            removalList.clear();
            for (Instruction instr : executeList) {
                if (instr.getRemainingCycles() == 1) {
                    removalList.add(instr);
                    instr.advanceState();
                    // update dest in register file
                    if (instr.getDest() != -1 && RFTags[instr.getDest()] == instr.getTag()) {
                        rFile.markReady(instr.getDest());
                    }
                    // wake up dependent instructions
                    for (Instruction entry : fakeROB.entries) {
                        if (entry.getSrc1() == instr.getTag() && entry.getIsSrc1Tagged()) {
                            entry.setSrc1Ready();
                        }
                        if (entry.getSrc2() == instr.getTag() && entry.getIsSrc2Tagged()) {
                            entry.setSrc2Ready();
                        }
                    }
                } else {
                    instr.decrementCycle();
                }
            }
            executeList.removeAll(removalList);

            // -------------------
            // 3) Issue()
            // -------------------
            int issueCount = N + 1;
            removalList.clear();
            for (Instruction instr : issueList) {
                if (instr.isReady() && issueCount > 0) {
                    schedulingCount--;
                    issueCount--;
                    removalList.add(instr);
                    executeList.add(instr);
                    instr.advanceState();
                }
            }
            issueList.removeAll(removalList);

            // -------------------
            // 4) Dispatch()
            // -------------------
            removalList.clear();
            for (Instruction instr : dispatchList) {
                if (instr.getState() == Instruction.State.ID && schedulingCount < S) {
                    schedulingCount++;
                    dispatchCount--;
                    removalList.add(instr);
                    issueList.add(instr);
                    // ID -> IS transition
                    instr.advanceState();

                    // handle register readiness
                    if (instr.getSrc1() != -1) {
                        if (rFile.isReady(instr.getSrc1())) {
                            instr.setSrc1Ready();
                        } else {
                            instr.setSrc1NotReady();
                            instr.setSrc1(RFTags[instr.getSrc1()]);
                            instr.setIsSrc1Tagged(true);
                        }
                    }
                    if (instr.getSrc2() != -1) {
                        if (rFile.isReady(instr.getSrc2())) {
                            instr.setSrc2Ready();
                        } else {
                            instr.setSrc2NotReady();
                            instr.setSrc2(RFTags[instr.getSrc2()]);
                            instr.setIsSrc2Tagged(true);
                        }
                    }
                    if (instr.getDest() != -1) {
                        RFTags[instr.getDest()] = instr.getTag();
                        rFile.markNotReady(instr.getDest());
                    }
                }
                // IF -> ID transition (1-cycle model for IF)
                if (instr.getState() == Instruction.State.IF) {
                    instr.advanceState();
                }
            }
            dispatchList.removeAll(removalList);

            // -------------------
            // 5) Fetch or Insert NOP?
            // -------------------
            int fetchCount = N; // how many instructions we pull each cycle
            while (fetchCount > 0 && dispatchCount < maxDispatchSize && s.hasNext()) {
                Instruction instr = createInstruction(s);
                fetchCount--;
                tag++;
                fakeROB.addInstruction(instr);
                dispatchList.add(instr);
                dispatchCount++;

                wattageSum += instr.getWattage();
                total_wattage += instr.getWattage();
                instrCount++;

                if (instrCount == checkN) {
                    // we've reached 'n' instructions, check the wattage sum
                    if (wattageSum > threshold) {
                        // Insert 10 NOP instructions
                        for (int i = 0; i < 10; i++) {
                            // create a “NOP” instruction
                            //   PC = "NOP"
                            //   op code = 0 (or anything you prefer, but 0 is simplest)
                            //   dest = -1, src1 = -1, src2 = -1
                            Instruction nop = new Instruction("NOP", 0, -1, -1, -1);
                            tag++;
                            fakeROB.addInstruction(nop);
                            dispatchList.add(nop);
                            dispatchCount++;
                        }
                    }
                    // Reset for the next group
                    instrCount = 0;
                    wattageSum = 0;
                }
            }

            // -------------------
            // 6) Update timing
            // -------------------
            for (Instruction instr : fakeROB.entries) {
                instr.updateTiming(cycle);
            }

            // end-of-cycle
            cycle++;
        } while (!fakeROB.isEmpty() || s.hasNext());

        cycle--;
        System.out.println(String.format("%-25s= %d", "number of instructions", tag));
        System.out.println(String.format("%-25s= %d", "number of cycles", cycle));
        System.out.println(String.format("%-25s= %.5f", "IPC", (float) tag / cycle));
        System.out.println(String.format("%-25s= %.5f", "average wattage per instruction", (float) total_wattage / tag));
        s.close();
    }
}
