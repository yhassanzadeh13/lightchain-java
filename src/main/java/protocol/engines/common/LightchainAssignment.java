package protocol.engines.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import model.lightchain.Assignment;
import model.lightchain.Identifier;
import protocol.Parameters;
import protocol.Tags;
import protocol.assigner.ValidatorAssigner;
import state.Snapshot;

public class LightchainAssignment {

  /**
   * Performs Lightchain validator assigner on the given identifier and returns validator assignment.
   *
   * @param id identifier of entity that urges validator assignment.
   * @param assigner the validator assignment logic.
   * @param snapshot snapshot of protocol state from which validators are picked.
   * @return list of validators assigned to this entity.
   * @throws IllegalStateException
   */
  public static Assignment getValidators(
      Identifier id,
      ValidatorAssigner assigner,
      Snapshot snapshot) throws IllegalStateException {

    byte[] bytesId = id.getBytes();
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
