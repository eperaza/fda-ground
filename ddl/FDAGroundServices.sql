USE [FDAGroundServices]
GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[flight_records](
	[flight_record_name] [nvarchar](255) NOT NULL,
	[storage_path] [nvarchar](255) NOT NULL,
	[file_size_kb] [int] NOT NULL,
	[flight_datetime] [datetimeoffset](7) NOT NULL,
	[aid_id] [nvarchar](255) NULL,
	[airline] [nvarchar](64) NOT NULL,
	[user_id] [nvarchar](255) NOT NULL,
	[status] [nvarchar](64) NOT NULL,
	[upload_to_adw] [bit] NOT NULL,
	[deleted_on_aid] [bit] NOT NULL,
	[processed_by_analytics] [bit] NOT NULL,
	[create_ts] [datetimeoffset](7) NOT NULL,
	[update_ts] [datetimeoffset](7) NULL,
 CONSTRAINT [PK_flight_records] PRIMARY KEY CLUSTERED 
(
	[flight_record_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON),
)
GO

ALTER TABLE [dbo].[flight_records] ADD  CONSTRAINT [DF_flight_records_status]  DEFAULT ('UPLOADED') FOR [status]
GO

ALTER TABLE [dbo].[flight_records] ADD  CONSTRAINT [DF_flight_records_upload_to_adw]  DEFAULT ((0)) FOR [upload_to_adw]
GO

ALTER TABLE [dbo].[flight_records] ADD  CONSTRAINT [DF_flight_records_deleted_on_aid]  DEFAULT ((0)) FOR [deleted_on_aid]
GO

ALTER TABLE [dbo].[flight_records] ADD  CONSTRAINT [DF_flight_records_processed_by_analytics]  DEFAULT ((0)) FOR [processed_by_analytics]
GO

ALTER TABLE [dbo].[flight_records] ADD  CONSTRAINT [DF_flight_records_CreateTS]  DEFAULT (sysutcdatetime()) FOR [create_ts]
GO

CREATE NONCLUSTERED INDEX [IX_flight_records_flight_datetime] ON [dbo].[flight_records]
(
	[flight_datetime] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
GO

CREATE TRIGGER [dbo].[FlightRecords_Trigger_UpdateTS]
ON [dbo].[flight_records]
AFTER INSERT, UPDATE
AS BEGIN

       SET NOCOUNT ON;

       UPDATE FR
              SET FR.update_ts = SYSUTCDATETIME()
       FROM flight_records AS FR
              INNER JOIN inserted AS I
                     ON FR.flight_record_name = I.flight_record_name;

END
GO

ALTER TABLE [dbo].[flight_records] ENABLE TRIGGER [FlightRecords_Trigger_UpdateTS]
GO

CREATE TABLE [dbo].[user_account_registrations](
        [registration_token] [nvarchar](64) NOT NULL,
        [user_object_id] [nvarchar](64) NOT NULL,
        [user_principal_name] [nvarchar](100) NOT NULL,
        [airline] [nvarchar](50) NOT NULL,
        [work_email] [nvarchar](100) NOT NULL,
        [account_state] [nvarchar](32) NOT NULL,
        [create_ts] [datetime2](7) NOT NULL,
        [update_ts] [datetime2](7) NULL,
 CONSTRAINT [UNQ_user_account_registrations_RT] UNIQUE NONCLUSTERED
(       
        [registration_token] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
)
GO

ALTER TABLE [dbo].[user_account_registrations] ADD  CONSTRAINT [DF_user_account_registrations_AccountState]  DEFAULT ('PENDING_USER_ACTIVATION') FOR [account_state]
GO

ALTER TABLE [dbo].[user_account_registrations] ADD  CONSTRAINT [DF_user_account_registrations_CreateTS]  DEFAULT (sysutcdatetime()) FOR [create_ts]
GO

CREATE TRIGGER UserAccountReg_Trigger_UpdateTS
ON user_account_registrations
AFTER INSERT, UPDATE
AS BEGIN

	SET NOCOUNT ON;

	UPDATE TN
		SET TN.update_ts = SYSUTCDATETIME()
	FROM user_account_registrations AS TN
		INNER JOIN inserted AS I
			ON TN.registration_token = I.registration_token;

END
GO

ALTER TABLE [dbo].[user_account_registrations] ENABLE TRIGGER [UserAccountReg_Trigger_UpdateTS]
GO
