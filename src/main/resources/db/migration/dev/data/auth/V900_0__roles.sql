INSERT INTO roles (role_id, role_code, role_name, create_datetime, role_description, admin_type)
VALUES ('a7f59e44-cbf0-4913-ba11-0764f39c06c2', 'ADD_SENSITIVE_CASE_NOTES', 'Add Secure Case Notes', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('bf263e33-601b-4558-b6b7-28a0349f669b', 'APPROVE_CATEGORISATION', 'Approve Category assessments', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('4f108240-992c-4a56-b64a-bc5d8b285c81', 'IMS_USER', 'IMS user', '2021-10-15 21:35:49.520000', null, 'DPS_ADM,DPS_LSA,IMS_HIDDEN'),
       ('0a09f0a0-2562-4f14-a2fa-fb1349687032', 'AUDIT_VIEWER', 'Audit viewer', '2021-10-15 21:35:51.130000', null, 'EXT_ADM'),
       ('d1ac4fec-be51-4302-aa52-31c095fd2d14', 'AUTH_GROUP_MANAGER', 'Auth Group Manager', '2021-10-15 21:35:48.406667', null, 'EXT_ADM'),
       ('fb0ffd90-01ad-4248-9800-2067ef5580ea', 'CATEGORISATION_SECURITY', 'Security Cat tool role', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('5a5d8fbe-9159-45e6-9758-5df571293587', 'CENTRAL_ADMIN', 'All Powerful Admin', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('6bade6d6-c705-11ec-9d64-0242ac120002', 'CMD_MIGRATED_MFA', 'Check my diary', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,EXT_ADM'),
       ('0688e81d-60fd-4002-85bc-f3ebc677ccd2', 'CREATE_CATEGORISATION', 'Create Category assessments', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('24aa9805-fb4b-4b64-ab58-90e8b95f5953', 'CREATE_USER', 'Create DPS Users', '2021-10-15 21:35:52.056667', 'Create different types of DPS users', 'DPS_ADM'),
       ('8b48546c-82a6-4553-9957-0f7d58625431', 'CRS_PROVIDER', 'Commissioned rehabilitative services (CRS) provider', '2021-10-15 21:35:51.173333', null, 'EXT_ADM'),
       ('3e2f0d25-948f-4591-a78a-a58f07982cb5', 'GLOBAL_APPOINTMENT', 'Global Appointment', '2021-10-15 21:35:48.940000', null, 'EXT_ADM'),
       ('6b84d235-710c-4112-a449-7045d5d9cb35', 'GLOBAL_SEARCH', 'Global Search', '2021-10-15 21:35:48.406667', 'Allow user to search globally for a user', 'DPS_ADM,EXT_ADM'),
       ('73c5a7d1-5b8d-498e-9aa9-037f0e787b3a', 'HMPPS_REGISTERS_MAINTAINER', 'HMPPS Registers Maintainer', '2021-10-15 21:35:50.990000', null, 'DPS_ADM,EXT_ADM'),
       ('84427bbf-32c9-4c18-8762-4f986d96aac6', 'HPA_USER', 'Historical Prisoner Application User', '2021-10-15 21:35:50.990000', null, 'DPS_ADM'),
       ('43225e5f-8729-4eff-a2c1-b537b3327322', 'HWPV_BAND_9', 'HWPV Band 9', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('7d42fcf0-06a4-4d2d-877f-c57b20625282', 'HWPV_CLAIM_ENTRY_BAND_2', 'HWPV Band 2', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('4185f06a-dac4-4672-8449-5e607b576085', 'HWPV_CASEWORK_MANAGER_BAND_5', 'HWPV Band 5', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('49f8cf8e-e8b7-40ef-bdbd-4c77557b6e35', 'HWPV_CLAIM_PAYMENT_BAND_3', 'HWPV Band 3', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('7264b2a6-fc99-4efc-bbae-fc43e9942780', 'HWPV_SSCL', 'Help with Prison Visits SSCL User', '2021-10-15 21:35:51.366667', null, 'EXT_ADM'),
       ('6fd11e65-a015-446c-ab86-4040f4e64d65', 'HWPV_SSCL_USER', 'HWPV SSCL', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('1e304461-c5a1-458c-9b86-1d85bce36a1b', 'INACTIVE_BOOKINGS', 'View Inactive Bookings', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('bf06bf48-7985-4af4-bbdf-e94d8e438f7d', 'KW_MIGRATION', 'KW Migration', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('725d8193-0eeb-4fcc-ad4a-5192d123c8f7', 'LICENCE_CA', 'Licence Case Admin', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('d0cbb927-068b-4a38-957a-6a5eea403a40', 'LICENCE_READONLY', 'Licence read only', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('f0167703-ea41-4b1b-af73-f48f1f0b340e', 'LICENCE_DM', 'Licence Decision Maker', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('fed13351-3f33-49d0-a61f-f952cf2989d1', 'LICENCE_RO', 'Licence Responsible Officer', '2021-10-15 21:35:48.406667', null, 'DPS_ADM,DPS_LSA,EXT_ADM'),
       ('7c5b1183-9f15-4dab-91f5-cbdf33f21e82', 'LICENCE_RO_READONLY', 'Licence RO Read Only', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('4f382076-45ab-44d1-8af0-d8c53b5affda', 'LICENCE_VARY', 'Licence Variation', '2021-10-15 21:35:48.406667', null, 'EXT_ADM'),
       ('73c5a7d1-5b8d-498e-9aa9-037f0e78793a', 'MAKE_RECALL_DECISION', 'Making a Recall Decision User', '2021-10-15 21:35:50.990000', null, 'EXT_ADM'),
       ('0a81eb06-b142-4e7f-85b7-d61ef17f57e6', 'MANAGE_RECALLS', 'PPUD Manage Recalls', '2021-10-15 21:35:51.086667', null, 'DPS_ADM,EXT_ADM'),
       ('f42befde-f1be-4243-afb7-ad4f28516050', 'MAINTAIN_EMAIL_DOMAINS', 'Maintain Email Domains for HMPPS Auth', '2021-10-15 21:35:51.366667', null, 'EXT_ADM'),
       ('97fd770e-ee92-4a61-8268-0c633ba84957', 'MAINTAIN_OAUTH_USERS', 'Maintain Auth Users (admin)', '2021-10-15 21:35:48.406667', null, 'DPS_ADM,EXT_ADM'),
       ('ef6d4384-c49c-4cc5-a5f9-38d0b11b6a38', 'MAINTAIN_IEP', 'Maintain IEP', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('d265c4f7-8bd7-4981-a592-ae133039c905', 'MAINTAIN_ACCESS_ROLES_ADMIN', 'Maintain Access Roles Admin', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('c7467eb7-3a0f-42b6-bec6-ef06a0d58143', 'MFA', 'Multi Factor Authentication', '2021-10-15 21:35:48.823333', 'Enforces MFA/2FA on an individual user', 'EXT_ADM'),
       ('54d1d4fe-6058-49ef-ab24-c1d006f899a6', 'MMP_READER', 'Manage my Prison Reader', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('d6295bb7-ef88-4fd1-9d61-c1329570219a', 'NOMIS_BATCHLOAD', 'Licence Batch Load (admin)', '2021-10-15 21:35:48.406667', null, 'DPS_ADM,EXT_ADM'),
       ('1711cbe7-46a3-496c-9b69-99bfbd73ccfe', 'OAUTH_ADMIN', 'Auth Client Management (admin)', '2021-10-15 21:35:48.406667', null, 'DPS_ADM,EXT_ADM'),
       ('8ce3c1de-cba8-11ec-9d64-0242ac120002', 'OAUTH_VIEW_ONLY_CLIENT', 'View Clients', '2021-10-15 21:35:52.056667', 'View Client details', 'EXT_ADM'),
       ('f42befdb-f1bd-4243-afb7-fd4828516050', 'OMIC_ADMIN', 'Omic Administrator', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('a81d769f-d215-433a-8435-c9d11852617f', 'PCMS_ANALYST', 'PCMS Analyst', '2021-10-15 21:35:50.906667', null, 'DPS_ADM,EXT_ADM'),
       ('3199653b-b305-4a69-9494-37cd71b7c4d8', 'PCMS_AUDIT', 'PCMS Audit', '2021-10-15 21:35:51.250000', null, 'DPS_ADM,EXT_ADM'),
       ('4d5ac52d-fe4b-49a7-8595-5da9af550943', 'PCMS_AUTHORISING_OFFICER', 'PCMS Authorising Officer', '2021-10-15 21:35:50.906667', null, 'DPS_ADM,EXT_ADM'),
       ('e97ff485-dd2c-471a-858d-79b95e38f042', 'PCMS_GLOBAL_ADMIN', 'PCMS Global Admin', '2021-10-15 21:35:50.906667', null, 'DPS_ADM,EXT_ADM'),
       ('a1c9fe1d-8606-4bc2-8a72-dc1e4489b36b', 'PECS_CDM', 'PECS Contract Delivery Manager', '2021-10-15 21:35:50.730000', null, 'EXT_ADM'),
       ('c3f66ba8-9b07-4c66-82db-8004cc2fac19', 'PECS_COURT', 'PECS Court User', '2021-10-15 21:35:49.476667', null, 'EXT_ADM'),
       ('cc08b1ea-01b1-4815-a4da-824e2642a43d', 'PECS_JPC', 'PECS Journey Price Calculation', '2021-10-15 21:35:50.136667', null, 'EXT_ADM'),
       ('edd815d6-009c-40a7-a852-b41eb0268642', 'PECS_MAINTAIN_PRICE', 'PECS Journey Price Maintenance', '2021-10-15 21:35:50.926667', null, 'EXT_ADM'),
       ('e6a53633-7e45-4b42-b6a8-e95ab1039d8d', 'PECS_PER_AUTHOR', 'PECS Person Escort Record Author', '2021-10-15 21:35:49.476667', null, 'EXT_ADM'),
       ('063a6459-7130-4b63-a542-b4ea3e1f691c', 'PECS_POLICE', 'PECS Police', '2021-10-15 21:35:48.406667', null, 'EXT_ADM'),
       ('2fe24f4c-fc21-4e89-927a-408629ba85b7', 'PECS_READ_ONLY', 'PECS Read Only', '2021-10-15 21:35:51.070000', null, 'EXT_ADM'),
       ('c6121107-8f4f-4a05-b75d-4752f473845b', 'PECS_SUPPLIER', 'PECS Supplier', '2021-10-15 21:35:48.406667', null, 'EXT_ADM'),
       ('b8c59140-a804-4cdb-9436-804d8f7c892a', 'PECS_SYSTEM_ADMIN', 'PECS System Administrator', '2021-10-15 21:35:50.730000', null, 'EXT_ADM'),
       ('befe3087-ca07-4b06-a7fc-97d7917871df', 'PF_APPROVAL', 'Pathfinder Approval', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('7fdf6d22-263c-4e3f-88a6-f45019bf72e2', 'PF_HQ', 'Pathfinder HQ User', '2021-10-15 21:35:49.500000', null, 'DPS_ADM,DPS_LSA,EXT_ADM'),
       ('0b1c65f4-dca1-4500-8e91-69822a3e5579', 'PF_LOCAL_READER', 'Pathfinder Local Reader', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('4ffb8463-8f5c-4bf1-babf-37ab17fe5f12', 'PF_NATIONAL_READER', 'Pathfinder National Reader', '2021-10-15 21:35:50.120000', null, 'DPS_ADM,DPS_LSA,EXT_ADM'),
       ('d179afdd-6f9c-4788-a02d-9ee3ba4a600d', 'PF_POLICE', 'Pathfinder Police', '2021-10-15 21:35:49.296667', null, 'EXT_ADM'),
       ('59a76913-1625-450f-9b69-9a746bfea40f', 'PF_STD_PRISON', 'Pathfinder Standard Prison', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('76fbf3ca-40e4-4933-8f84-3c59f4e1dd75', 'PF_STD_PRISON_RO', 'Pathfinder Prison Read Only', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('e437722e-fef7-41af-8a85-012a32912732', 'POM', 'Prisoner Offender Manager', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('16e2d62d-d95e-419b-9637-ba315e8378ad', 'ROLES_ADMIN', 'role administrator', '2021-10-15 21:35:51.386667', 'Role to create roles and update role name and description', 'EXT_ADM'),
       ('411d4c11-37a0-4e0f-b574-22c22b6da4b0', 'SOC_COMMUNITY', 'SOC Probation Role', '2021-10-15 21:35:49.430000', null, 'EXT_ADM'),
       ('fa81950f-5329-4597-af89-b29bfd3ed247', 'SOC_CUSTODY', 'SOC Prison Role', '2021-10-15 21:35:49.430000', null, 'DPS_ADM,DPS_LSA,EXT_ADM'),
       ('07023294-ae59-4680-8fb4-f6e811baf0ab', 'SOC_HQ', 'SOC Headquarters User', '2021-10-15 21:35:50.416667', null, 'EXT_ADM'),
       ('b6674e53-13da-4497-a3b3-c12d04faf75d', 'SYSTEM_READ_ONLY', 'System Read Only', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('2498e369-d8c7-4bef-bf82-99d405a920e2', 'UNIT_TEST_DPS_ROLE', 'Test Role DPS', '2021-10-15 21:35:52.056667', 'DPS Role for unit tests', 'DPS_ADM'),
       ('276eaa7a-c8ad-4e5d-826f-beb1f5733146', 'USE_OF_FORCE_COORDINATOR', 'Use of force coordinator', '2021-10-15 21:35:52.056667', null, 'DPS_ADM'),
       ('5f73771c-a965-4194-8705-454c78ff2c20', 'USE_OF_FORCE_REVIEWER', 'Use of force reviewer', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA'),
       ('83e27e2f-989f-44aa-b91a-61c2010471bf', 'VIDEO_LINK_COURT_USER', 'Video Link Court User', '2021-10-15 21:35:48.843333', null, 'EXT_ADM'),
       ('789a6389-54a6-4538-9d54-8a5eb7af0be4', 'VIEW_SENSITIVE_CASE_NOTES', 'View Secure Case Notes', '2021-10-15 21:35:52.056667', null, 'DPS_ADM,DPS_LSA');


INSERT INTO roles (role_id, role_code, role_name, create_datetime, role_description, admin_type, hidden_date)
VALUES ('a7f59e44-cbf0-4913-ba11-0764f39caaaa', 'HIDDEN_ROLE', 'Role hidden, will not be returned in get roles', '2025-01-25 23:35:52.000000', null, 'DPS_ADM,DPS_LSA','2025-01-30 00:00:00.000000');