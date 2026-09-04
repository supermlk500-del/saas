-- Add the process-center capability menu without changing business data.
INSERT INTO sys_menu
    (menuId, menuKey, menuName, parentId, orderNum, path, component, menuType, visible, status, perms, icon, deleted, remark)
VALUES
    (4003, 'master-capability', '设备能力', 4000, 3, '/master/capability', NULL, 'C', '0', '0', 'process:machine:list', NULL, 0, '工序与设备能力关系维护')
ON DUPLICATE KEY UPDATE
    menuName = VALUES(menuName),
    parentId = VALUES(parentId),
    orderNum = VALUES(orderNum),
    path = VALUES(path),
    menuType = VALUES(menuType),
    visible = VALUES(visible),
    status = VALUES(status),
    perms = VALUES(perms),
    deleted = 0,
    remark = VALUES(remark);

INSERT IGNORE INTO sys_role_menu (roleId, menuId) VALUES
    (1, 4003),
    (4, 4003),
    (8, 4003);
