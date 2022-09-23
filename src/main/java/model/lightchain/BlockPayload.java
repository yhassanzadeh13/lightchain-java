package model.lightchain;

import java.util.Arrays;

import model.Entity;
import model.codec.EntityType;

/**
 * Models payload of the blocks in LightChain.
 */
public class BlockPayload extends Entity {
  /**
   * Set of validated transactions that this block carries.
   */
  private final ValidatedTransaction[] transactions;

  /**
   * Constructor of block payload.
   *
   * @param transactions list of transactions in this block payload.
   */
  public BlockPayload(ValidatedTransaction[] transactions) {
    this.transactions = transactions.clone();
  }

  public ValidatedTransaction[] getTransactions() {
    return transactions.clone();
  }

  @Override
  public String type() {
    return EntityType.TYPE_BLOCK_PAYLOAD;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BlockPayload)) {
      return false;
    }
    BlockPayload that = (BlockPayload) o;
    return Arrays.equals(getTransactions(), that.getTransactions());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(getTransactions());
  }
}
