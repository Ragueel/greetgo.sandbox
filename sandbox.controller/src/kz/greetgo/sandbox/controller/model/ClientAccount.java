package kz.greetgo.sandbox.controller.model;

public class ClientAccount {
  public int id;
  public int clientId;
  public float money;
  public String number;
  public String registeredAt;

  public ClientAccount(int id, float money){
    this.id = id;
    this.money = money;
  }
}
