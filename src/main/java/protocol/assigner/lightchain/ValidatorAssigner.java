package protocol.assigner.lightchain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import model.lightchain.Assignment;
import model.lightchain.Identifier;
import protocol.Parameters;
import protocol.Tags;
import protocol.assigner.AssignerInf;
import state.Snapshot;

/**
 * Implements assigner logic for the validators of an entity.
 */
// TODO: this class should be covered with testing.
public class ValidatorAssigner implements protocol.assigner.ValidatorAssigner {
  private final AssignerInf assigner;

  public ValidatorAssigner() {
    this.assigner = new Assigner();
  }

  /**
   * Returns the validators of the given entity.
   *
   * @param identifier identifier of entity that urges validator assignment.
   * @param snapshot   snapshot of protocol state from which validators are picked.
   * @return list of validators assigned to this entity.
   * @throws IllegalStateException any unhappy path taken on computing validators.
   */
  @Override
  public Assignment getValidatorsAtSnapshot(Identifier identifier, Snapshot snapshot) throws IllegalStateException {
    byte[] bytesId = identifier.getBytes();
    byte[] bytesTag = Tags.ValidatorTag.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      output.write(bytesId, 0, 32);
      output.write(bytesTag);
    } catch (IOException e) {
      throw new IllegalStateException("could not write to bytes to ByteArrayOutputStream", e);
    }
    Identifier taggedId = new Identifier(output.toByteArray());
    return assigner.assign(taggedId, snapshot, Parameters.VALIDATOR_THRESHOLD);
  }
}
