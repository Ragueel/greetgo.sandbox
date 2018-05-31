package kz.greetgo.sandbox.controller.controller;

import kz.greetgo.depinject.core.Bean;
import kz.greetgo.depinject.core.BeanGetter;
import kz.greetgo.mvc.annotations.*;
import kz.greetgo.sandbox.controller.model.AuthInfo;
import kz.greetgo.sandbox.controller.model.Client;
import kz.greetgo.sandbox.controller.model.RecordClient;
import kz.greetgo.sandbox.controller.model.UserInfo;
import kz.greetgo.sandbox.controller.register.AuthRegister;
import kz.greetgo.sandbox.controller.security.NoSecurity;
import kz.greetgo.sandbox.controller.util.Controller;

import java.util.List;

/**
 * как составлять контроллеры написано
 * <a href="https://github.com/greetgo/greetgo.mvc/blob/master/greetgo.mvc.parent/doc/controller_spec.md">здесь</a>
 */
@Bean
@Mapping("/auth")
public class AuthController implements Controller {

    public BeanGetter<AuthRegister> authRegister;

    @AsIs
    @NoSecurity
    @Mapping("/login")
    public String login(@Par("accountName") String accountName, @Par("password") String password) {
        return authRegister.get().login(accountName, password);
    }

    @ToJson
    @Mapping("/info")
    public AuthInfo info(@ParSession("personId") String personId) {
        return authRegister.get().getAuthInfo(personId);
    }

    @ToJson
    @Mapping("/userInfo")
    public UserInfo userInfo(@ParSession("personId") String personId) {
        return authRegister.get().getUserInfo(personId);
    }

    @ToJson
    @Mapping("/clients")
    public List<RecordClient> getClients(@Par("paginationPage") String paginationPage) {
        return authRegister.get().getClients(paginationPage);
    }

    @ToJson
    @Mapping("/delete")
    public boolean deleteClient(@Par("index") String index,
                                @Par("paginationPage") String paginationPage) {
        System.out.println("Value: " + index);
        authRegister.get().deleteClient(index);
        return false;
    }

    @ToJson
    @Mapping("/search")
    public List<RecordClient> searchClient(@Par("searchName") String searchName) {
        return authRegister.get().searchClient(searchName);
    }

    @ToJson
    @Mapping("/add_client")
    public boolean addClient(@Par("newClient") String newClient) {
        return authRegister.get().addNewClient(newClient);
    }

    @ToJson
    @Mapping("/sort")
    public List<RecordClient> sortClientByColumnNum(@Par("columnNum") String columnNum,
                                                    @Par("paginationPage") String paginationPage) {
        System.out.println("Inside Controller:" + columnNum);
        return authRegister.get().sortClientByColumnNum(columnNum, paginationPage);
    }

    @ToJson
    @Mapping("/pagination_page_num")
    public int getPaginationNum() {
        return authRegister.get().getPaginationNum();
    }

    @ToJson
    @Mapping("/getClientWithId")
    public Client getClientById(@Par("clientId") String clientId) {
        System.out.println("Retrieving from Controller: " + clientId);
        return authRegister.get().getClientById(clientId);
    }
}
