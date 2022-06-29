package protocol.assigner.lightchain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import model.lightchain.Assignment;
import model.lightchain.Identifier;
import protocol.Tags;
import state.Snapshot;

// TODO: develop tests for this class.
public class LcProposerAssigner implements protocol.assigner.ProposerAssigner {
  private final protocol.assigner.Assigner assigner;

  public LcProposerAssigner() {
    this.assigner = new LcAssigner();
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
