/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.services.security;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.mail.internet.MimeUtility;
import javax.servlet.ServletConfig;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.om.profile.Layout;
import org.apache.jetspeed.om.profile.Portlets;
import org.apache.jetspeed.om.profile.Profile;
import org.apache.jetspeed.om.profile.ProfileLocator;
import org.apache.jetspeed.om.profile.psml.PsmlLayout;
import org.apache.jetspeed.om.security.BaseJetspeedUser;
import org.apache.jetspeed.om.security.Group;
import org.apache.jetspeed.om.security.JetspeedUser;
import org.apache.jetspeed.om.security.JetspeedUserFactory;
import org.apache.jetspeed.om.security.Role;
import org.apache.jetspeed.om.security.UserIdPrincipal;
import org.apache.jetspeed.om.security.UserNamePrincipal;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.Profiler;
import org.apache.jetspeed.services.PsmlManager;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.services.rundata.JetspeedRunDataService;
import org.apache.jetspeed.services.security.CredentialsManagement;
import org.apache.jetspeed.services.security.JetspeedSecurityException;
import org.apache.jetspeed.services.security.JetspeedSecurityService;
import org.apache.jetspeed.services.security.NotUniqueUserException;
import org.apache.jetspeed.services.security.UnknownUserException;
import org.apache.jetspeed.services.security.UserException;
import org.apache.jetspeed.services.security.UserManagement;
import org.apache.turbine.om.security.User;
import org.apache.turbine.services.InitializationException;
import org.apache.turbine.services.TurbineBaseService;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.localization.Localization;
import org.apache.turbine.services.resources.ResourceService;
import org.apache.turbine.services.rundata.RunDataService;
import org.apache.turbine.util.RunData;

import com.aimluck.eip.cayenne.om.account.EipMUserPosition;
import com.aimluck.eip.cayenne.om.security.TurbineGroup;
import com.aimluck.eip.cayenne.om.security.TurbineRole;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.cayenne.om.security.TurbineUserGroupRole;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;

/**
 * ユーザーを管理するクラスです。 <br />
 *
 */
public class ALUserManagement extends TurbineBaseService implements
    UserManagement, CredentialsManagement {

  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALUserManagement.class.getName());

  private final static String CONFIG_SECURE_PASSWORDS_KEY = "secure.passwords";

  private final static String CONFIG_SECURE_PASSWORDS_ALGORITHM =
    "secure.passwords.algorithm";

  private final static String CONFIG_SYSTEM_USERS = "system.users";

  boolean securePasswords = false;

  String passwordsAlgorithm = "SHA";

  Vector<?> systemUsers = null;

  private final static String CONFIG_NEWUSER_ROLES = "newuser.roles";

  private final static String CONFIG_NEW_ADMINUSER_ROLES = "newadminuser.roles";

  private final static String[] DEFAULT_CONFIG_NEWUSER_ROLES = { "user" };

  private final static String[] DEFAULT_CONFIG_NEW_ADMINUSER_ROLES = {
    "user",
    "admin" };

  String roles[] = null;

  String admin_roles[] = null;

  private JetspeedRunDataService runDataService = null;

  protected JetspeedUser row2UserObject(TurbineUser tuser) throws UserException {
    try {
      JetspeedUser user = JetspeedUserFactory.getInstance(false);
      ALBaseUser baseuser = (ALBaseUser) user;
      baseuser.setUserId(tuser.getUserId().toString());
      baseuser.setUserName(tuser.getLoginName());
      baseuser.setPassword(tuser.getPasswordValue());
      baseuser.setFirstName(tuser.getFirstName());
      baseuser.setLastName(tuser.getLastName());
      baseuser.setEmail(tuser.getEmail());
      baseuser.setConfirmed(tuser.getConfirmValue());
      baseuser.setModified(tuser.getModified());
      baseuser.setCreated(tuser.getCreated());
      baseuser.setLastLogin(tuser.getLastLogin());
      // baseuser.setDisabled("T".equals(tuser.getDisabled()));
      baseuser.setDisabled(tuser.getDisabled());
      // baseuser.setObjectdata(null);
      baseuser.setPasswordChanged(tuser.getPasswordChanged());
      baseuser.setCompanyId((tuser.getCompanyId() != null) ? tuser
        .getCompanyId()
        .intValue() : 0);
      baseuser.setPositionId((tuser.getPositionId() != null) ? tuser
        .getPositionId()
        .intValue() : 0);
      baseuser.setInTelephone(tuser.getInTelephone());
      baseuser.setOutTelephone(tuser.getOutTelephone());
      baseuser.setCellularPhone(tuser.getCellularPhone());
      baseuser.setCellularMail(tuser.getCellularMail());
      baseuser.setCelluarUId(tuser.getCellularUid());
      baseuser.setLastNameKana(tuser.getLastNameKana());
      baseuser.setFirstNameKana(tuser.getFirstNameKana());
      baseuser.setPhoto(tuser.getPhoto());
      baseuser.setCreatedUserId((tuser.getCreatedUserId() != null) ? tuser
        .getCreatedUserId()
        .intValue() : 0);
      baseuser.setUpdatedUserId((tuser.getUpdatedUserId() != null) ? tuser
        .getUpdatedUserId()
        .intValue() : 0);
      return baseuser;
    } catch (Exception e) {
      logger.error(e);
      return null;
    }
  }

  /**
   *
   */
  public JetspeedUser getUser(Principal principal)
      throws JetspeedSecurityException {

    SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
    if (principal instanceof UserNamePrincipal) {
      Expression exp =
        ExpressionFactory.matchExp(TurbineUser.LOGIN_NAME_PROPERTY, principal
          .getName());
      query.setQualifier(exp);
    } else if (principal instanceof UserIdPrincipal) {
      Expression exp =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(principal.getName()));
      query.setQualifier(exp);
    } else {
      throw new UserException("Invalid Principal Type in getUser: "
        + principal.getClass().getName());
    }
    List<TurbineUser> users;
    try {
      users = query.fetchList();
    } catch (Exception e) {
      String message = "Failed to retrieve user '" + principal.getName() + "'";
      logger.error(message, e);
      throw new UserException(message, e);
    }
    if (users.size() > 1) {
      throw new UserException("Multiple Users with same username '"
        + principal.getName()
        + "'");
    }
    if (users.size() == 1) {
      try {
        JetspeedUser juser = row2UserObject(users.get(0));
        return juser;
      } catch (UserException e) {
        String message =
          "Failed to retrieve user '" + principal.getName() + "'";
        logger.error(message, e);
        throw new UserException(message, e);
      }
    }
    throw new UnknownUserException("Unknown user '" + principal.getName() + "'");
  }

  /**
   *
   */
  public JetspeedUser getUser(RunData rundata, Principal principal)
      throws JetspeedSecurityException {

    SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
    if (principal instanceof UserNamePrincipal) {
      Expression exp =
        ExpressionFactory.matchExp(TurbineUser.LOGIN_NAME_PROPERTY, principal
          .getName());
      query.setQualifier(exp);
    } else if (principal instanceof UserIdPrincipal) {
      Expression exp =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(principal.getName()));
      query.setQualifier(exp);
    } else {
      throw new UserException("Invalid Principal Type in getUser: "
        + principal.getClass().getName());
    }
    List<TurbineUser> users;
    try {
      users = query.fetchList();
    } catch (Exception e) {
      String message = "Failed to retrieve user '" + principal.getName() + "'";
      logger.error(message, e);
      throw new UserException(message, e);
    }
    if (users.size() > 1) {
      throw new UserException("Multiple Users with same username '"
        + principal.getName()
        + "'");
    }
    if (users.size() == 1) {
      try {
        JetspeedUser juser = row2UserObject(users.get(0));
        return juser;
      } catch (UserException e) {
        String message =
          "Failed to retrieve user '" + principal.getName() + "'";
        logger.error(message, e);
        throw new UserException(message, e);
      }
    }
    throw new UnknownUserException("Unknown user '" + principal.getName() + "'");
  }

  /**
   *
   */
  public Iterator<JetspeedUser> getUsers() throws JetspeedSecurityException {
    List<JetspeedUser> users = new ArrayList<JetspeedUser>();
    try {

      List<TurbineUser> list = Database.query(TurbineUser.class).fetchList();
      for (TurbineUser user : list) {
        users.add(row2UserObject(user));
      }
    } catch (Exception e) {
      logger.error("Failed to retrieve users ", e);
      throw new UserException("Failed to retrieve users ", e);
    }
    return users.iterator();
  }

  /**
   *
   */
  public Iterator<JetspeedUser> getUsers(String filter)
      throws JetspeedSecurityException {

    List<JetspeedUser> users = new ArrayList<JetspeedUser>();
    try {
      List<TurbineUser> list = Database.query(TurbineUser.class).fetchList();
      for (TurbineUser user : list) {
        users.add(row2UserObject(user));
      }
    } catch (Exception e) {
      logger.error("Failed to retrieve users ", e);
      throw new UserException("Failed to retrieve users ", e);
    }
    return users.iterator();
  }

  /**
   *
   */
  public void saveUser(JetspeedUser user) throws JetspeedSecurityException {
    if (!accountExists(user, true)) {
      throw new UnknownUserException("Cannot save user '"
        + user.getUserName()
        + "', User doesn't exist");
    }

    try {

      Boolean hasAdminCredential = (Boolean) user.getPerm("isAdmin", null);

      Expression exp =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(user.getUserId()));

      List<TurbineUser> users =
        Database.query(TurbineUser.class, exp).fetchList();

      if (users == null || users.size() == 0) {
        throw new UnknownUserException("Cannot save user '"
          + user.getUserName()
          + "', User doesn't exist");
      }

      ALBaseUser baseuser = (ALBaseUser) user;
      // 新規オブジェクトモデル
      TurbineUser tuser = users.get(0);
      tuser.setLoginName(baseuser.getUserName());
      tuser.setPasswordValue(baseuser.getPassword());
      tuser.setFirstName(baseuser.getFirstName());
      tuser.setLastName(baseuser.getLastName());
      tuser.setEmail(baseuser.getEmail());
      tuser.setConfirmValue(baseuser.getConfirmed());
      tuser.setModified(baseuser.getCreateDate());
      tuser.setCreated(baseuser.getCreateDate());
      tuser.setLastLogin(baseuser.getCreateDate());
      // tuser.setDisabled((baseuser.getDisabled() ? "T" : "F"));
      tuser.setDisabled(baseuser.getDisabled());
      tuser.setObjectdata(null);
      tuser.setPasswordChanged(baseuser.getPasswordChanged());
      tuser.setCompanyId(Integer.valueOf(baseuser.getCompanyId()));
      tuser.setPositionId(Integer.valueOf(baseuser.getPositionId()));
      tuser.setInTelephone(baseuser.getInTelephone());
      tuser.setOutTelephone(baseuser.getOutTelephone());
      tuser.setCellularPhone(baseuser.getCellularPhone());
      tuser.setCellularMail(baseuser.getCellularMail());
      tuser.setCellularUid(baseuser.getCelluarUId());
      tuser.setLastNameKana(baseuser.getLastNameKana());
      tuser.setFirstNameKana(baseuser.getFirstNameKana());
      tuser.setPhoto(baseuser.getPhoto());
      tuser.setCreatedUserId(Integer.valueOf(baseuser.getCreatedUserId()));
      tuser.setUpdatedUserId(Integer.valueOf(baseuser.getUpdatedUserId()));

      if (hasAdminCredential != null) {
        if (hasAdminCredential) {
          setAdminRole(tuser);
          addAdminPage(tuser.getLoginName());
        } else {
          removeAdminRole(tuser);
          removeAdminPage(tuser.getLoginName());
        }
        grantRoles(user, hasAdminCredential);
      }

      // ユーザを更新する．
      Database.commit();
    } catch (Exception e) {
      logger.error("Failed to save user object ", e);
      throw new UserException("Failed to save user object ", e);
    }

  }

  /**
   *
   */
  public void addUser(JetspeedUser user) throws JetspeedSecurityException {
    if (accountExists(user)) {
      throw new NotUniqueUserException("The account '"
        + user.getUserName()
        + "' already exists");
    }

    boolean hasAdminCredential = (Boolean) user.getPerm("isAdmin", false);

    String initialPassword = user.getPassword();
    String encrypted = JetspeedSecurity.encryptPassword(initialPassword);
    user.setPassword(encrypted);

    ALBaseUser baseuser = (ALBaseUser) user;
    // 新規オブジェクトモデル
    TurbineUser tuser = Database.create(TurbineUser.class);
    tuser.setLoginName(baseuser.getUserName());
    tuser.setPasswordValue(baseuser.getPassword());
    tuser.setFirstName(baseuser.getFirstName());
    tuser.setLastName(baseuser.getLastName());
    tuser.setEmail(baseuser.getEmail());
    tuser.setConfirmValue(baseuser.getConfirmed());
    tuser.setModified(baseuser.getCreateDate());
    tuser.setCreated(baseuser.getCreateDate());
    tuser.setLastLogin(baseuser.getCreateDate());
    // tuser.setDisabled((baseuser.getDisabled() ? "T" : "F"));
    tuser.setDisabled(baseuser.getDisabled());
    tuser.setObjectdata(null);
    tuser.setPasswordChanged(baseuser.getPasswordChanged());
    tuser.setCompanyId(Integer.valueOf(baseuser.getCompanyId()));
    tuser.setPositionId(Integer.valueOf(baseuser.getPositionId()));
    tuser.setInTelephone(baseuser.getInTelephone());
    tuser.setOutTelephone(baseuser.getOutTelephone());
    tuser.setCellularPhone(baseuser.getCellularPhone());
    tuser.setCellularMail(baseuser.getCellularMail());
    // tuser.setCellularUid();
    tuser.setLastNameKana(baseuser.getLastNameKana());
    tuser.setFirstNameKana(baseuser.getFirstNameKana());
    tuser.setPhoto(baseuser.getPhoto());
    tuser.setCreatedUserId(Integer.valueOf(baseuser.getCreatedUserId()));
    tuser.setUpdatedUserId(Integer.valueOf(baseuser.getUpdatedUserId()));

    Database.commit();

    // ログインユーザーにはグループ LoginUser に所属させる
    Group group = JetspeedSecurity.getGroup("LoginUser");
    Role role = JetspeedSecurity.getRole("user");
    // 新規オブジェクトモデル
    TurbineUserGroupRole user_group_role =
      Database.create(TurbineUserGroupRole.class);
    user_group_role.setTurbineUser(tuser);
    user_group_role.setTurbineGroup((TurbineGroup) group);
    user_group_role.setTurbineRole((TurbineRole) role);

    if (hasAdminCredential) {
      // 管理者ロール付与
      setAdminRole(tuser);
    }

    // 役職に登録
    List<EipMUserPosition> userposlist =
      Database.query(EipMUserPosition.class).fetchList();
    int new_pos =
      (userposlist != null && userposlist.size() > 0)
        ? userposlist.size() + 1
        : 1;
    EipMUserPosition userposition = Database.create(EipMUserPosition.class);
    userposition.setTurbineUser(tuser);
    userposition.setPosition(Integer.valueOf(new_pos));

    try {
      // ユーザを登録
      Database.commit();

      ((BaseJetspeedUser) user).setUserId(tuser.getUserId().toString());

    } catch (Exception e) {
      Database.rollback();
      String message = "Failed to create account '" + user.getUserName() + "'";
      logger.error(message, e);
      throw new UserException(message, e);
    }

    addDefaultPSML(user, hasAdminCredential);
  }

  /**
   * 指定したユーザーにデフォルトのPSMLを設定します。
   *
   * @param user
   * @throws JetspeedSecurityException
   */
  private void addDefaultPSML(JetspeedUser user, boolean hasAdminCredential)
      throws JetspeedSecurityException {
    String orgId = Database.getDomainName();

    grantRoles(user, hasAdminCredential);

    try {
      JetspeedRunData rundata = getRunData();
      if (rundata != null && Profiler.useRoleProfileMerging() == false) {
        Profile profile = Profiler.createProfile();
        profile.setUser(user);
        profile.setMediaType("html");
        profile.setOrgName(orgId);
        Profiler.createProfile(getRunData(), profile);

        if (hasAdminCredential) {
          addAdminPage(user);
        }
      }
    } catch (Exception e) {
      logger.error("Failed to create profile for new user ", e);
      removeUser(new UserNamePrincipal(user.getUserName()));
      throw new UserException("Failed to create profile for new user ", e);
    }
  }

  /**
   * ユーザーのロールを承認します
   *
   * @param user
   * @param hasAdminCredential
   */
  private void grantRoles(JetspeedUser user, boolean hasAdminCredential) {
    String _roles[] = null;
    if (hasAdminCredential) {
      _roles = admin_roles;
    } else {
      _roles = roles;
    }

    for (int i = 0; i < _roles.length; i++) {
      try {
        JetspeedSecurity.grantRole(user.getUserName(), JetspeedSecurity
          .getRole(_roles[i])
          .getName());
      } catch (Exception e) {
        logger.error("Could not grant role: "
          + _roles[i]
          + " to user "
          + user.getUserName(), e);
      }
    }
  }

  /**
   * 指定したユーザのPSMLにシステム管理のページを追加します。
   *
   * @param user_name
   * @throws Exception
   */
  private void addAdminPage(String user_name) throws Exception {
    ProfileLocator locator = Profiler.createLocator();
    locator.createFromPath(String.format("user/%s/media-type/html", user_name));
    Portlets portlets =
      Profiler.getProfile(locator).getDocument().getPortlets();

    if (portlets != null) {
      // 既に配置されているかどうかを確認
      int portlet_size = portlets.getPortletsCount();
      for (int i = 0; i < portlet_size; i++) {
        Portlets p = portlets.getPortlets(i);
        if (p.getSecurityRef().getParent().equals("admin-view")) {
          return;
        }
      }

      // レイアウトの作成
      Layout newLayout = new PsmlLayout();
      newLayout.setPosition(portlet_size);
      newLayout.setSize(-1);

      ProfileLocator admin_locator = Profiler.createLocator();
      admin_locator.createFromPath("user/admin/media-type/html");
      Portlets admin_portlets =
        Profiler.createProfile(admin_locator).getDocument().getPortlets();
      admin_portlets = admin_portlets.getPortlets(0);
      admin_portlets.setLayout(newLayout);
      portlets.addPortlets(admin_portlets);
    }
  }

  /**
   * 指定したユーザのPSMLにシステム管理のページを追加します。
   *
   * @param user
   * @throws Exception
   */
  private void addAdminPage(User user) throws Exception {
    addAdminPage(user.getUserName());
  }

  /**
   * 指定したユーザのPSMLからシステム管理のページを取り除きます。
   *
   * @param user
   * @throws Exception
   */
  private void removeAdminPage(String user_name) throws Exception {
    ProfileLocator locator = Profiler.createLocator();
    locator.createFromPath(String.format("user/%s/media-type/html", user_name));
    Portlets portlets =
      Profiler.getProfile(locator).getDocument().getPortlets();
    List<Integer> remove_index = new ArrayList<Integer>();
    if (portlets != null) {
      int portlet_size = portlets.getPortletsCount();
      for (int i = 0; i < portlet_size; i++) {
        Portlets p = portlets.getPortlets(i);
        if (p.getSecurityRef().getParent().equals("admin-view")) {
          portlets.removePortlets(i);
        }
      }
      Collections.reverse(remove_index);
      for (Integer index : remove_index) {
        portlets.removePortlets(index);
      }
    }
  }

  /**
   * 指定したユーザに管理者権限を付与します。
   *
   * @param tuser
   * @throws JetspeedSecurityException
   */
  private void setAdminRole(TurbineUser tuser) throws JetspeedSecurityException {
    Role adminrole = JetspeedSecurity.getRole("admin");
    Group group = JetspeedSecurity.getGroup("LoginUser");
    // 新規オブジェクトモデル
    TurbineUserGroupRole admin_group_role =
      Database.create(TurbineUserGroupRole.class);
    admin_group_role.setTurbineUser(tuser);
    admin_group_role.setTurbineGroup((TurbineGroup) group);
    admin_group_role.setTurbineRole((TurbineRole) adminrole);
  }

  /**
   * 指定したユーザの管理者権限を取り除きます。
   *
   * @param tuser
   * @throws JetspeedSecurityException
   */
  @SuppressWarnings("unchecked")
  private void removeAdminRole(TurbineUser tuser)
      throws JetspeedSecurityException {
    String admin_role_id = JetspeedSecurity.getRole("admin").getId();
    List<TurbineUserGroupRole> user_roles = tuser.getTurbineUserGroupRole();
    for (TurbineUserGroupRole role : user_roles) {
      if (role.getTurbineRole().getId().equals(admin_role_id)) {
        Database.delete(role);
      }
    }
    Database.commit();
  }

  /**
   *
   */
  public void removeUser(Principal principal) throws JetspeedSecurityException {
    if (systemUsers.contains(principal.getName())) {
      throw new UserException("["
        + principal.getName()
        + "] is a system user and cannot be removed");
    }

    JetspeedUser user = getUser(principal);

    try {

      Expression exp =
        ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, Integer
          .valueOf(user.getUserId()));

      List<TurbineUser> users =
        Database.query(TurbineUser.class, exp).fetchList();

      if (users == null || users.size() == 0) {
        throw new UserException("["
          + principal.getName()
          + "] is a system user and cannot be removed");
      }

      Database.delete(users.get(0));
      PsmlManager.removeUserDocuments(user);

      Database.commit();
    } catch (Exception e) {
      Database.rollback();
      String message = "Failed to remove account '" + user.getUserName() + "'";
      logger.error(message, e);
      throw new UserException(message, e);
    }
  }

  /**
   *
   */
  public void changePassword(JetspeedUser user, String oldPassword,
      String newPassword) throws JetspeedSecurityException {
    oldPassword = JetspeedSecurity.convertPassword(oldPassword);
    newPassword = JetspeedSecurity.convertPassword(newPassword);

    String encrypted = JetspeedSecurity.encryptPassword(oldPassword);
    if (!accountExists(user)) {
      throw new UnknownUserException(Localization
        .getString("UPDATEACCOUNT_NOUSER"));
    }
    if (!user.getPassword().equals(encrypted)) {
      throw new UserException(Localization
        .getString("UPDATEACCOUNT_BADOLDPASSWORD"));
    }
    user.setPassword(JetspeedSecurity.encryptPassword(newPassword));

    user.setPasswordChanged(new Date());

    saveUser(user);
  }

  /**
   *
   */
  public void forcePassword(JetspeedUser user, String password)
      throws JetspeedSecurityException {
    if (!accountExists(user)) {
      throw new UnknownUserException("The account '"
        + user.getUserName()
        + "' does not exist");
    }
    user.setPassword(JetspeedSecurity.encryptPassword(password));
    saveUser(user);
  }

  /**
   *
   */
  public String encryptPassword(String password)
      throws JetspeedSecurityException {
    if (securePasswords == false) {
      return password;
    }
    if (password == null) {
      return null;
    }

    try {
      if ("SHA-512".equals(passwordsAlgorithm)) {
        // パスワード末尾にencrypt_keyを付加
        password = password + JetspeedResources.getString("aipo.encrypt_key");

        MessageDigest md = MessageDigest.getInstance(passwordsAlgorithm);
        md.reset();
        md.update(password.getBytes());
        byte[] hash = md.digest();
        // ハッシュを16進数文字列に変換
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
          sb.append(Integer.toHexString((hash[i] >> 4) & 0x0F));
          sb.append(Integer.toHexString(hash[i] & 0x0F));
        }
        return sb.toString();
      } else {
        // SHA-512以外のアルゴリズムの場合は、以下の処理でパスワードを暗号化する。

        MessageDigest md = MessageDigest.getInstance(passwordsAlgorithm);
        // We need to use unicode here, to be independent of platform's
        // default encoding. Thanks to SGawin for spotting this.
        byte[] digest =
          md.digest(password.getBytes(ALEipConstants.DEF_CONTENT_ENCODING));
        ByteArrayOutputStream bas =
          new ByteArrayOutputStream(digest.length + digest.length / 3 + 1);
        OutputStream encodedStream = MimeUtility.encode(bas, "base64");
        encodedStream.write(digest);
        encodedStream.flush();
        encodedStream.close();
        return bas.toString();
      }
    } catch (Exception e) {
      logger.error("Unable to encrypt password." + e.getMessage(), e);
      return null;
    }
  }

  /**
   *
   */
  @Override
  public synchronized void init(ServletConfig conf)
      throws InitializationException {
    if (getInit()) {
      return;
    }

    super.init(conf);

    ResourceService serviceConf =
      ((TurbineServices) TurbineServices.getInstance())
        .getResources(JetspeedSecurityService.SERVICE_NAME);

    securePasswords =
      serviceConf.getBoolean(CONFIG_SECURE_PASSWORDS_KEY, securePasswords);
    passwordsAlgorithm =
      serviceConf.getString(
        CONFIG_SECURE_PASSWORDS_ALGORITHM,
        passwordsAlgorithm);
    systemUsers =
      serviceConf.getVector(CONFIG_SYSTEM_USERS, new Vector<Object>());

    try {
      roles = serviceConf.getStringArray(CONFIG_NEWUSER_ROLES);
      admin_roles = serviceConf.getStringArray(CONFIG_NEW_ADMINUSER_ROLES);
    } catch (Exception e) {
    }

    if (null == roles || roles.length == 0) {
      roles = DEFAULT_CONFIG_NEWUSER_ROLES;
    }

    if (null == admin_roles || admin_roles.length == 0) {
      admin_roles = DEFAULT_CONFIG_NEW_ADMINUSER_ROLES;
    }

    this.runDataService =
      (JetspeedRunDataService) TurbineServices.getInstance().getService(
        RunDataService.SERVICE_NAME);

    setInit(true);
  }

  /**
   *
   * @param user
   * @return
   * @throws UserException
   */
  protected boolean accountExists(JetspeedUser user) throws UserException {
    return accountExists(user, false);
  }

  protected boolean accountExists(JetspeedUser user, boolean checkUniqueId)
      throws UserException {
    String id = user.getUserId();

    Expression exp =
      ExpressionFactory.matchExp(TurbineUser.LOGIN_NAME_PROPERTY, user
        .getUserName());

    List<TurbineUser> users;
    try {
      users = Database.query(TurbineUser.class, exp).fetchList();
    } catch (Exception e) {
      logger.error("Failed to check account's presence", e);
      throw new UserException("Failed to check account's presence", e);
    }
    if (users.size() < 1) {
      return false;
    }
    TurbineUser retrieved = users.get(0);
    String keyId = retrieved.getUserId().toString();
    if (checkUniqueId && !keyId.equals(id)) {
      throw new UserException("User exists but under a different unique ID");
    }
    return true;
  }

  protected JetspeedRunData getRunData() {
    JetspeedRunData rundata = null;
    if (this.runDataService != null) {
      rundata = this.runDataService.getCurrentRunData();
    }
    return rundata;
  }

}
