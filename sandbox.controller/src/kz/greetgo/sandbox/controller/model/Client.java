package kz.greetgo.sandbox.controller.model;

public class Client {

    public int id;
    public String name;
    public String surname;
    public String patronymic;
    public String gender;
    public String birthDate;

    public String snmn = "Empty";
    public int age = 0;
    public int accBalance = 10;
    public int maxBalance = 0;
    public int minBalance = 0;

    public Character charm;

    // Addresses
    public Address[] addresses;

    // Phone numbers
    public Phone[] phones;
}
