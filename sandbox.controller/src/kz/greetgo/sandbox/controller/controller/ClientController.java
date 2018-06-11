package kz.greetgo.sandbox.controller.controller;

import kz.greetgo.depinject.core.Bean;
import kz.greetgo.depinject.core.BeanGetter;
import kz.greetgo.mvc.annotations.*;
import kz.greetgo.mvc.core.RequestMethod;
import kz.greetgo.sandbox.controller.model.Character;
import kz.greetgo.sandbox.controller.model.ClientDetails;
import kz.greetgo.sandbox.controller.model.ClientToSave;
import kz.greetgo.sandbox.controller.model.RecordClient;
import kz.greetgo.sandbox.controller.register.ClientRegister;
import kz.greetgo.sandbox.controller.util.Controller;

import java.util.List;

@Bean
@Mapping("/client")
public class ClientController implements Controller {

  public BeanGetter<ClientRegister> clientRegister;

  @ToJson
  @MethodFilter(RequestMethod.DELETE)
  @Mapping("/delete")
  public boolean delete(@Par("index") String index) {
    clientRegister.get().deleteClient(index);
    return false;
  }

  @ToJson
  @MethodFilter(RequestMethod.POST)
  @Mapping("/save")
  public int save(@Json @Par("editedClient") ClientToSave editedClient) {
    return clientRegister.get().editedClient(editedClient);
  }

  @ToJson
  @MethodFilter(RequestMethod.GET)
  @Mapping("/getPaginationNum")
  public int getPaginationNum(@Par("searchText") String searchText, @Par("sliceNum") int sliceNum) {
    return clientRegister.get().getRequestedPaginationNum(searchText, sliceNum);
  }

  @ToJson
  @MethodFilter(RequestMethod.GET)
  @Mapping("/getClients")
  public List<RecordClient> getClients(@Par("columnName") String columnName,
                                       @Par("paginationPage") String paginationPage,
                                       @Par("searchName") String searchName,
                                       @Par("sliceNum") int sliceNum) {
    return clientRegister.get().getClients(columnName, paginationPage, searchName, sliceNum);
  }

  @ToJson
  @MethodFilter(RequestMethod.GET)
  @Mapping("/getClientWithId")
  public ClientDetails getClientById(@Par("clientId") String clientId) {
    return clientRegister.get().getClientById(Integer.parseInt(clientId));
  }

  @ToJson
  @MethodFilter(RequestMethod.GET)
  @Mapping("/characters")
  public List<Character> getCharacters() {
    return clientRegister.get().getCharacters();
  }

}
