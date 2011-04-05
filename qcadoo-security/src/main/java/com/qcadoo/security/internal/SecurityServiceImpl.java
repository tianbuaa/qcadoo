/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.3.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */

package com.qcadoo.security.internal;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.aop.Monitorable;
import com.qcadoo.model.api.search.Restrictions;
import com.qcadoo.plugin.api.profile.Standalone;
import com.qcadoo.security.api.SecurityService;

@Service("userDetailsService")
@Standalone
public class SecurityServiceImpl implements SecurityService, UserDetailsService, PersistentTokenRepository {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Override
    @Monitorable
    public String getCurrentUserName() {
        return getUserEntity(SecurityContextHolder.getContext().getAuthentication().getName()).getStringField("userName");
    }

    protected Entity getUserEntity(final String login) {
        List<Entity> users = dataDefinitionService.get("qcadooSecurity", "user").find()
                .restrictedWith(Restrictions.eq("userName", login)).withMaxResults(1).list().getEntities();
        checkState(users.size() > 0, "Current user with login %s cannot be found", login);
        return users.get(0);
    }

    @Override
    @Monitorable
    public Long getCurrentUserId() {
        return getUserEntity(SecurityContextHolder.getContext().getAuthentication().getName()).getId();
    }

    @Override
    @Monitorable
    public UserDetails loadUserByUsername(final String username) {
        return convertEntityToUserDetails(getUserEntity(username));
    }

    protected UserDetails convertEntityToUserDetails(final Entity entity) {
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        authorities.add(new GrantedAuthorityImpl(entity.getBelongsToField("userGroup").getStringField("role")));

        checkState(authorities.size() > 0, "Current user with login %s cannot be found", entity.getStringField("userName"));

        return new User(entity.getStringField("userName"), entity.getStringField("password"), true, true, true, true, authorities);
    }

    @Override
    public void createNewToken(final PersistentRememberMeToken token) {
        Entity entity = dataDefinitionService.get("qcadooSecurity", "persistentToken").create();
        entity.setField("userName", token.getUsername());
        entity.setField("series", token.getSeries());
        entity.setField("token", token.getTokenValue());
        entity.setField("lastUsed", token.getDate());
        dataDefinitionService.get("qcadooSecurity", "persistentToken").save(entity);
    }

    @Override
    public void updateToken(final String series, final String tokenValue, final Date lastUsed) {
        Entity entity = getPersistentToken(series);
        if (entity != null) {
            entity.setField("token", tokenValue);
            entity.setField("lastUsed", lastUsed);
            dataDefinitionService.get("qcadooSecurity", "persistentToken").save(entity);
        }
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(final String series) {
        Entity entity = getPersistentToken(series);

        if (entity != null) {
            return new PersistentRememberMeToken(entity.getStringField("userName"), entity.getStringField("series"),
                    entity.getStringField("token"), (Date) entity.getField("lastUsed"));
        } else {
            return null;
        }
    }

    @Override
    public void removeUserTokens(final String username) {
        List<Entity> entities = dataDefinitionService.get("qcadooSecurity", "persistentToken").find()
                .restrictedWith(Restrictions.eq("userName", username)).list().getEntities();

        for (Entity entity : entities) {
            dataDefinitionService.get("qcadooSecurity", "persistentToken").delete(entity.getId());
        }
    }

    private Entity getPersistentToken(final String series) {
        List<Entity> entities = dataDefinitionService.get("qcadooSecurity", "persistentToken").find()
                .restrictedWith(Restrictions.eq("series", series)).list().getEntities();

        if (entities.size() == 1) {
            return entities.get(0);
        } else {
            return null;
        }
    }

}
