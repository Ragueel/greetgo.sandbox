package kz.greetgo.sandbox.db.stand.beans;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.greetgo.depinject.core.Bean;
import kz.greetgo.depinject.core.HasAfterInject;
import kz.greetgo.sandbox.controller.model.*;
import kz.greetgo.sandbox.controller.model.Character;
import kz.greetgo.sandbox.db.stand.model.PersonDot;
import kz.greetgo.util.RND;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Bean
public class StandDb implements HasAfterInject {
    Random random;

    public final Map<String, PersonDot> personStorage = new HashMap<>();
    public final List<Client> clientDotList = new ArrayList<>();

    public final Character[] characters = {
            new Character(),
            new Character(),
            new Character(),
            new Character(),
            new Character()};

    private final int sliceNum = 10;

    @Override
    public void afterInject() throws Exception {
        random = new Random();

        initCharacters();
        initClientList();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("StandDbInitData.txt"), "UTF-8"))) {

            int lineNo = 0;

            while (true) {
                System.out.println("Check logs");
                String line = br.readLine();
                if (line == null) break;
                lineNo++;
                String trimmedLine = line.trim();
                if (trimmedLine.length() == 0) continue;
                if (trimmedLine.startsWith("#")) continue;

                String[] splitLine = line.split(";");
                String command = splitLine[0].trim();
                switch (command) {
                    case "PERSON":
                        appendPerson(splitLine, line, lineNo);
                        break;

                    default:
                        throw new RuntimeException("Unknown command " + command);
                }
            }
        }
    }

    private void initCharacters() {
        characters[0].name = "Angry";
        characters[1].name = "Scrappy";
        characters[2].name = "Cold-blooded";
        characters[3].name = "Careful";
        characters[4].name = "Relaxed";
    }

    public int getPaginationNum() {
        return clientDotList.size() / sliceNum + clientDotList.size() % sliceNum;
    }

    private void initClientList() {
        for (int i = 0; i < 100; i++) {
            Client d = new Client();

            d.id = i;
            d.name = RND.str(10);
            d.surname = RND.str(10);
            d.patronymic = RND.str(10);

            d.snmn = d.name + " " + d.surname + " " + d.patronymic;

            d.snmn = RND.str(50);
            d.age = random.nextInt(50) + 18;
            d.charm = giveRandomCharacter();

            d.addresses = createRandomAddresses();
            d.phones = createRandomPhoneNumbers();

            clientDotList.add(d);

            System.out.println(d);
        }
    }

    private Address[] createRandomAddresses() {
        Address[] addresses = new Address[2];
        addresses[0] = new Address();
        addresses[0].street = RND.str(10);
        addresses[0].flat = RND.str(10);

        addresses[1] = new Address();
        addresses[1].street = RND.str(10);
        addresses[1].flat = RND.str(10);

        return addresses;
    }

    private Phone[] createRandomPhoneNumbers() {
        Phone[] phones = new Phone[3];
        phones[0] = new Phone();
        phones[0].number = RND.str(10);

        phones[1] = new Phone();
        phones[1].number = RND.str(10);

        phones[2] = new Phone();
        phones[2].number = RND.str(10);


        return phones;
    }

    private Character giveRandomCharacter() {
        int randNum = random.nextInt(characters.length);
        return characters[randNum];
    }

    public List<RecordClient> getClientSlice(String paginationPage) {
        List<RecordClient> clients = new ArrayList<>();
        int currentPagination = Integer.parseInt(paginationPage);
        int startSlice = currentPagination * sliceNum;
        int endSlice = sliceNum * currentPagination;
        endSlice += sliceNum;

        if (endSlice > clientDotList.size()) {
            endSlice = clientDotList.size();
        }
        for (int i = startSlice; i < endSlice; i++) {
            RecordClient client = new RecordClient();
            client.id = clientDotList.get(i).id;
            client.name = clientDotList.get(i).name;
            client.surname = clientDotList.get(i).surname;
            client.patronymic = clientDotList.get(i).patronymic;
            client.age = clientDotList.get(i).age;

            client.accBalance = clientDotList.get(i).accBalance;
            client.maxBalance = clientDotList.get(i).maxBalance;
            clients.add(client);
        }

        return clients;
    }

    public List<RecordClient> sortClientByColumnNum(int columnNum, String paginationPage) {
        List<RecordClient> sortedList = getClientSlice(paginationPage);
        System.out.println("Column header num:" + columnNum);
        if (columnNum == 1) {
            System.out.println("Sorting with surname:");
            Collections.sort(sortedList, new Comparator<RecordClient>() {
                @Override
                public int compare(RecordClient o1, RecordClient o2) {
                    return o1.surname.compareTo(o2.surname);
                }
            });
        } else if (columnNum == 3) {
            Collections.sort(sortedList, new Comparator<RecordClient>() {
                @Override
                public int compare(RecordClient o1, RecordClient o2) {
                    return (o1.age > o2.age) ? 1 : ((o1.age < o2.age) ? -1 : 0);
                }
            });
        } else if (columnNum == 4) {
            Collections.sort(sortedList, new Comparator<RecordClient>() {
                @Override
                public int compare(RecordClient o1, RecordClient o2) {
                    return (o1.accBalance > o2.accBalance) ? 1 : ((o1.accBalance < o2.accBalance) ? -1 : 0);
                }
            });
        } else if (columnNum == 5) {
            Collections.sort(sortedList, new Comparator<RecordClient>() {
                @Override
                public int compare(RecordClient o1, RecordClient o2) {
                    return (o1.maxBalance > o2.maxBalance) ? 1 : ((o1.maxBalance < o2.maxBalance) ? -1 : 0);
                }
            });
        } else if (columnNum == 6) {
            Collections.sort(sortedList, new Comparator<RecordClient>() {
                @Override
                public int compare(RecordClient o1, RecordClient o2) {
                    return (o1.minBalance > o2.minBalance) ? 1 : ((o1.minBalance < o2.minBalance) ? -1 : 0);
                }
            });
        }
        for (RecordClient recordClient : sortedList) {
            System.out.println(recordClient.surname);
        }
        return sortedList;
    }

    public boolean addNewClient(String newClient) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(newClient);
            Client client = mapper.readValue(newClient, Client.class);

            clientDotList.add(client);
            for (Client cl : clientDotList) {
                System.out.println(cl);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean deleteClient(String clientId) {
        clientDotList.remove(Integer.parseInt(clientId));
        return true;
    }

    public Client getClientById(String clientId){
        int clientNum = Integer.parseInt(clientId);
        for (Client client: clientDotList) {
            if(client.id == clientNum){
                return client;
            }
        }
        return null;
    }

    public List<RecordClient> searchClient(String name) {
        List<RecordClient> searchClients = new ArrayList<>();
        System.out.println("ToSearch:" + name);
        if (name == null) {
            return getClientSlice("0");
        }
        for (Client client : clientDotList) {
            for (int i = 0; i < client.surname.length(); i++) {
                if (client.surname.charAt(i) == name.charAt(0)) {
                    System.out.println("Surname " + client.surname + " Search is" + name.charAt(0));
                }
            }
            if (client.surname.contains(name)) {
                RecordClient foundClient = new RecordClient();
                foundClient.id = client.id;
                foundClient.name = client.name;
                foundClient.surname = client.surname;
                foundClient.patronymic = client.patronymic;

                foundClient.age = client.age;
                foundClient.accBalance = client.accBalance;
                foundClient.maxBalance = client.maxBalance;
                searchClients.add(foundClient);
                System.out.println("Index is " + client.surname.indexOf(name.charAt(0)) + " " + client.name);
            }
        }
        return searchClients;
    }

    @SuppressWarnings("unused")
    private void appendPerson(String[] splitLine, String line, int lineNo) {
        PersonDot p = new PersonDot();
        p.id = splitLine[1].trim();
        String[] ap = splitLine[2].trim().split("\\s+");
        String[] fio = splitLine[3].trim().split("\\s+");
        p.accountName = ap[0];
        p.password = ap[1];
        p.surname = fio[0];
        p.name = fio[1];
        if (fio.length > 2) p.patronymic = fio[2];
        personStorage.put(p.id, p);
    }
}
