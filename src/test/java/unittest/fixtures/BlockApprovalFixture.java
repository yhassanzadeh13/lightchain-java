package unittest.fixtures;

import model.lightchain.BlockApproval;

/**
 * Creates block approval for a random block.
 */
public class BlockApprovalFixture {
  public static BlockApproval newBlockApproval() {
    return new BlockApproval(SignatureFixture.newSignatureFixture(), IdentifierFixture.newIdentifier());
  }
}
