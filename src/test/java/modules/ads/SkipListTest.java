package modules.ads;

import java.util.ArrayList;

import model.lightchain.Identifier;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.SkipListFixture;

public class SkipListTest {

  @Test
  public void tempTest() {
    ArrayList<Identifier> identifiers = new ArrayList<>();
    SkipListFixture skipListFixture = new SkipListFixture();
    EntityFixture entityFixture1 = new EntityFixture();
    EntityFixture entityFixture2 = new EntityFixture();
    EntityFixture entityFixture3 = new EntityFixture();
    EntityFixture entityFixture4 = new EntityFixture();
    EntityFixture entityFixture5 = new EntityFixture();

    System.out.println("--------1--------");
    identifiers.add(entityFixture1.id());
    skipListFixture.put(entityFixture1);
    identifiers.sort(Identifier::comparedTo);
    str(skipListFixture, identifiers);
    System.out.println("--------1--------");
    System.out.println("--------2--------");
    identifiers.add(entityFixture2.id());
    skipListFixture.put(entityFixture2);
    identifiers.sort(Identifier::comparedTo);
    str(skipListFixture, identifiers);
    System.out.println("--------2--------");
    System.out.println("--------3--------");
    identifiers.add(entityFixture3.id());
    skipListFixture.put(entityFixture3);
    identifiers.sort(Identifier::comparedTo);
    str(skipListFixture, identifiers);
    System.out.println("--------3--------");
    System.out.println("--------4--------");
    identifiers.add(entityFixture4.id());
    skipListFixture.put(entityFixture4);
    identifiers.sort(Identifier::comparedTo);
    str(skipListFixture, identifiers);
    System.out.println("--------4--------");
    System.out.println("--------5--------");
    identifiers.add(entityFixture5.id());
    skipListFixture.put(entityFixture5);
    identifiers.sort(Identifier::comparedTo);
    str(skipListFixture, identifiers);
    System.out.println("--------5--------");
  }
  public void str(SkipListFixture skipListFixture, ArrayList<Identifier> identifiers) {
    System.out.println(skipListFixture);
    System.out.print("srtd: ");
    for (Identifier identifier : identifiers) {
      System.out.print(identifier.toString().substring(0, 5));
      System.out.print(" ");
    }
    System.out.println();
  }

  // TODO: writing tests to cover
  // 1. When putting a unique entity into skip list, we can recover it.
  // 2. Proof of membership for putting and getting an entity is the same.
  // 3. Putting an already existing entity does not change its membership proof.
  // 4. Putting 100 distinct entities concurrently inserts all of them into skip list with correct membership proofs, and
  //    also, makes them all retrievable with correct membership proofs.
  // 5. Getting non-existing identifiers returns null.
  // 7. Putting null returns null.
  // 8. Tampering with root identifier of an authenticated entity fails its verification.
  // 9. Tampering with entity of an authenticated entity fails its verification.
  // 10. Tampering with proof of an authenticated entity fails its verification.
}
