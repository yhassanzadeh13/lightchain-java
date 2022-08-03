package protocol.assigner.lightchain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import model.lightchain.Assignment;
import model.lightchain.Identifier;
import protocol.Tags;
import protocol.assigner.AssignerInf;
import state.Snapshot;

/**
 * Implements finalita paper assigner logic for the next block proposer.
 */
// TODO: develop tests for this class.
public class FinalitaProposerAssigner implements protocol.assigner.ProposerAssigner {
  private final AssignerInf assigner;

  public FinalitaProposerAssigner() {
    this.assigner = new Assigner();
  }

  @Override
  public Identifier nextBlockProposer(Identifier currentBlockId, Snapshot s) throws IllegalArgumentException, IllegalStateException {
    byte[] bytesId = currentBlockId.getBytes();
    byte[] bytesTag = Tags.BlockProposerTag.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      output.write(bytesId, 0, bytesId.length);
      output.write(bytesTag);
    } catch (IOException e) {
      throw new IllegalStateException("could not write to bytes to ByteArrayOutputStream", e);
    }
    Identifier taggedId = new Identifier(output.toByteArray());

    // TODO: needs test cases to ensure correctness of choosing exactly one (at validator side).
    Assignment assignment = assigner.assign(taggedId, s, (short) 1);
    if (assignment.size() != 1) {
      throw new IllegalStateException("unexpected number of assigned proposers for the next block, expected 1, got: " + assignment.size());
    }
    return assignment.all().get(0);
  }
}
