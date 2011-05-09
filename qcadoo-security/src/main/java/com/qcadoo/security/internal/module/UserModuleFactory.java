package com.qcadoo.security.internal.module;

import static com.google.common.base.Preconditions.checkNotNull;

import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;

import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.plugin.api.ModuleFactory;

public class UserModuleFactory extends ModuleFactory<UserModule> {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Override
    public UserModule parse(final String pluginIdentifier, final Element element) {
        String login = element.getAttributeValue("login");
        String email = element.getAttributeValue("email");
        String firstName = element.getAttributeValue("firstName");
        String lastName = element.getAttributeValue("lastName");
        String password = element.getAttributeValue("password");
        String groupName = element.getAttributeValue("groupName");

        checkNotNull(login, "Missing login attribute of " + getIdentifier() + " module");
        checkNotNull(password, "Missing password attribute of " + getIdentifier() + " module");
        checkNotNull(groupName, "Missing groupName attribute of " + getIdentifier() + " module");

        return new UserModule(login, email, firstName, lastName, password, groupName, dataDefinitionService);
    }

    @Override
    public String getIdentifier() {
        return "user";
    }

}
