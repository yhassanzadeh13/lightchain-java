package unittest.fixtures;

import model.lightchain.BlockApproval;

public class BlockApprovalFixture {
  public static BlockApproval newBlockApproval() {
    return new BlockApproval(SignatureFixture.newSignatureFixture(), IdentifierFixture.newIdentifier());
  }
}
