package model.codec;

/**
 * Lists all legitimate types for encoding and decoding Entities of LightChain.
 */
public class EntityType {
  public static final String TYPE_TRANSACTION = "type-lightchain-transaction";
  public static final String TYPE_BLOCK = "type-lightchain-block";
  public static final String TYPE_BLOCK_PROPOSAL = "type-lightchain-block-proposal";
  public static final String TYPE_BLOCK_HEADER = "type-lightchain-block-header";
  public static final String TYPE_BLOCK_PAYLOAD = "type-lightchain-block-payload";
  public static final String TYPE_VALIDATED_TRANSACTION = "type-lightchain-validated-transaction";
  public static final String TYPE_VALIDATED_BLOCK = "type-lightchain-validated-block";
  public static final String TYPE_ECDSA_SIGNATURE = "type-lightchain-ecdsa-signature";
  public static final String TYPE_BLOCK_APPROVAL = "type-lightchain-block-approval";
}