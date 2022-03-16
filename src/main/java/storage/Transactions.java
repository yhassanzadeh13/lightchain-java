package storage;

import java.util.ArrayList;

import model.lightchain.Identifier;
import model.lightchain.Transaction;

public interface Transactions {
  boolean Has(Identifier transactionId);

  boolean Add(Transaction transaction);

  boolean Remove(Identifier transactionId);

  Transaction Get(Identifier transactionId);

  ArrayList<Transaction> All();
}
