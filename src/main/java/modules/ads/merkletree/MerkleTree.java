package modules.ads.merkletree;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedDataStructure;
import modules.ads.AuthenticatedEntity;
import org.apache.commons.collections.list.TreeList;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Implementation of an in-memory Authenticated Skip List
 * that is capable of storing and retrieval of LightChain entities.
 */
public class MerkleTree implements AuthenticatedDataStructure {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private MerkleNode root = new MerkleNode();
  private final ArrayList<MerkleNode> leafNodes = new ArrayList<>();
  private final ArrayList<Entity> entities = new ArrayList<>();

  public MerkleTree() {
  }

  @Override
  public AuthenticatedEntity put(Entity e) {
    entities.add(e);
    leafNodes.add(new MerkleNode(e, false));
    buildMerkleTree();
    printLevelOrderTraversal(root);
    return null;
  }

  @Override
  public AuthenticatedEntity get(Identifier id) {
    return null;
  }

  private void buildMerkleTree() {
    ArrayList<MerkleNode> parentNodes = new ArrayList<>();
    ArrayList<MerkleNode> childNodes = new ArrayList<>(leafNodes);
    while (childNodes.size() > 1) {
      int idx = 0;
      int len = childNodes.size();
      while (idx < len) {
        MerkleNode left = childNodes.get(idx);
        left.setLeft(true);
        MerkleNode right = null;
        if (idx + 1 < len) {
          right = childNodes.get(idx + 1);
        } else {
          right = new MerkleNode(left.getHash());
        }
        Sha3256Hash hash = hasher.computeHash(left.getHash().getBytes(), right.getHash().getBytes());
        MerkleNode parent = new MerkleNode(hash, left, right);
        left.setParent(parent);
        right.setParent(parent);
        parentNodes.add(parent);
        idx += 2;
      }
      childNodes = parentNodes;
      parentNodes = new ArrayList<>();
    }
    root = childNodes.get(0);
  }

  private static void printLevelOrderTraversal(MerkleNode r) {
    if (r == null) {
      return;
    }
    if ((r.getLeft() == null && r.getRight() == null)) {
      System.out.println(Arrays.toString(r.getHash().getBytes()));
    }
    Queue<MerkleNode> queue = new LinkedList<>();
    queue.add(r);
    queue.add(null);
    while (!queue.isEmpty()) {
      MerkleNode node = queue.poll();
      if (node != null) {
        System.out.println(Arrays.toString(node.getHash().getBytes()));
      } else {
        System.out.println();
        if (!queue.isEmpty()) {
          queue.add(null);
        }
      }
      if (node != null && node.getLeft() != null) {
        queue.add(node.getLeft());
      }
      if (node != null && node.getRight() != null) {
        queue.add(node.getRight());
      }
    }
  }
}
