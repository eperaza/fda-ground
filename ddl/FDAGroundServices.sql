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
        [display_name] [nvarchar](x) NULL,
        [first_name] [nvarchar](x) NULL,
        [last_name] [nvarchar](x) NULL,
        [email_address] [nvarchar](x) NULL,
        [user_role] [nvarchar](x) NULL,
        [registration_date] [nvarchar](x) NULL,
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

USE [FDAGroundServices]
GO

/****** Object:  Table [dbo].[supa_releases]    Script Date: 9/5/2018 12:40:24 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[supa_releases](
	[release] [nvarchar](64) NOT NULL,
	[part_number] [nvarchar](64) NOT NULL,
	[path] [nvarchar](128) NOT NULL,
	[airline] [nvarchar](64) NOT NULL,
	[create_ts] [datetimeoffset](7) NOT NULL,
	[update_ts] [datetimeoffset](7) NULL,
 CONSTRAINT [PK_supa_releases] PRIMARY KEY CLUSTERED 
(
	[release] ASC,
	[airline] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[supa_releases] ADD  CONSTRAINT [DF_supa_releases_CreateTS]  DEFAULT (sysutcdatetime()) FOR [create_ts]
GO


CREATE UNIQUE NONCLUSTERED INDEX [UNIQ_supa_releases_part_number_airline] ON [dbo].[supa_releases]
(
	[part_number] ASC,
	[airline] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

ALTER TABLE [dbo].[supa_releases] ADD  CONSTRAINT [DF_supa_releases_CreateTS]  DEFAULT (sysutcdatetime()) FOR [create_ts]
GO

CREATE TRIGGER [dbo].[SupaReleases_Trigger_UpdateTS]
ON [dbo].[supa_releases]
AFTER INSERT, UPDATE
AS BEGIN

       SET NOCOUNT ON;

       UPDATE SR
              SET SR.update_ts = SYSUTCDATETIME()
       FROM supa_releases AS SR
              INNER JOIN inserted AS I
                     ON SR.release = I.release
                        AND SR.airline = I.airline;

END
GO

ALTER TABLE [dbo].[supa_releases] ENABLE TRIGGER [SupaReleases_Trigger_UpdateTS]
GO

CREATE TABLE [dbo].[demo_flight_streams](
	[flight_stream_name] [nvarchar](64) NOT NULL,
	[path] [nvarchar](128) NOT NULL,
	[create_ts] [datetimeoffset](7) NOT NULL,
	[update_ts] [datetimeoffset](7) NULL
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
ALTER TABLE [dbo].[demo_flight_streams] ADD  CONSTRAINT [PK_demo_flight_streams] PRIMARY KEY CLUSTERED 
(
	[flight_stream_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
CREATE UNIQUE NONCLUSTERED INDEX [UNIQ_demo_flight_streams_path] ON [dbo].[demo_flight_streams]
(
	[path] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
ALTER TABLE [dbo].[demo_flight_streams] ADD  CONSTRAINT [DF_demo_flight_streams_CreateTS]  DEFAULT (sysutcdatetime()) FOR [create_ts]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TRIGGER [dbo].[DemoFlightStreams_Trigger_UpdateTS]
ON [dbo].[demo_flight_streams]
AFTER INSERT, UPDATE
AS BEGIN

       SET NOCOUNT ON;

       UPDATE DFS
              SET DFS.update_ts = SYSUTCDATETIME()
       FROM demo_flight_streams AS DFS
              INNER JOIN inserted AS I
                     ON DFS.flight_stream_name = I.flight_stream_name;

END

GO
ALTER TABLE [dbo].[demo_flight_streams] ENABLE TRIGGER [DemoFlightStreams_Trigger_UpdateTS]
GO



USE [FDAGroundServices]
GO

ALTER TABLE [dbo].[feature_management] DROP CONSTRAINT [DF_feature_management_create_ts]
GO

DROP INDEX airline1 ON feature_management
GO

/****** Object:  Table [dbo].[feature_management]    Script Date: 12/6/2017 1:18:20 PM ******/
DROP TABLE [dbo].[feature_management]
GO

/****** Object:  Table [dbo].[feature_management]    Script Date: 12/6/2017 1:18:20 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[feature_management](
	[id] [int] NOT NULL IDENTITY(1,1),
	[airline] [nvarchar](50) NOT NULL,
	[title] [nvarchar](180) NOT NULL,
	[feature_key] [nvarchar](220) NOT NULL,
	[description] [text] NOT NULL,
	[choice_pilot] [bit] NOT NULL,
	[choice_focal] [bit] NOT NULL,
	[choice_check_airman] [bit] NOT NULL,
	[choice_maintenance] [bit] NOT NULL,
	[updated_by] [nvarchar](220) NOT NULL,
	[create_ts] [datetime] NOT NULL,
	
PRIMARY KEY  
(
	[id] ASC
)) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

ALTER TABLE [dbo].[feature_management] ADD CONSTRAINT [DF_feature_management_create_ts]  DEFAULT (getdate()) FOR [create_ts]
GO

CREATE INDEX airline1 ON feature_management (airline);
GO
CREATE UNIQUE INDEX featurekey1 ON feature_management (airline, feature_key);
GO


USE [FDAGroundServices]
GO

ALTER TABLE [dbo].[airline_preferences] DROP CONSTRAINT [DF_airline_preferences_create_ts]
GO

DROP INDEX airline1 ON airline_preferences
GO

/****** Object:  Table [dbo].[airline_preferences]    Script Date: 12/6/2017 1:18:20 PM ******/
DROP TABLE [dbo].[airline_preferences]
GO

/****** Object:  Table [dbo].[airline_preferences]    Script Date: 12/6/2017 1:18:20 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[airline_preferences](
	[id] [int] NOT NULL IDENTITY(1,1),
	[airline] [nvarchar](50) NOT NULL,
	[preference] [nvarchar](220) NOT NULL,
	[airline_key] [nvarchar](220) NOT NULL,
	[description] [text] NOT NULL,
	[display] [bit] NOT NULL,
	[choice_pilot] [bit] NOT NULL,
	[choice_focal] [bit] NOT NULL,
	[choice_check_airman] [bit] NOT NULL,
	[choice_maintenance] [bit] NOT NULL,
	[updated_by] [nvarchar](220) NOT NULL,
	[create_ts] [datetime] NOT NULL,
	
PRIMARY KEY  
(
	[id] ASC
)) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

ALTER TABLE [dbo].[airline_preferences] ADD CONSTRAINT [DF_airline_preferences_create_ts]  DEFAULT (getdate()) FOR [create_ts]
GO

CREATE INDEX airline1 ON airline_preferences (airline);
GO
CREATE UNIQUE INDEX airlinekey1 ON airline_preferences (airline, airline_key);
GO



USE [FDAGroundServices]
GO

ALTER TABLE [dbo].[user_preferences] DROP CONSTRAINT [DF_user_preferences_create_ts]
GO

DROP INDEX airline1 ON user_preferences
GO

/****** Object:  Table [dbo].[user_preferences]    Script Date: 12/6/2017 1:18:20 PM ******/
DROP TABLE [dbo].[user_preferences]
GO

/****** Object:  Table [dbo].[user_preferences]    Script Date: 12/6/2017 1:18:20 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[user_preferences](
	[id] [int] NOT NULL IDENTITY(1,1),
	[airline] [nvarchar](50) NOT NULL,
	[preference] [nvarchar](220) NOT NULL,
	[user_key] [nvarchar](220) NOT NULL,
	[description] [text] NOT NULL,
	[groupby] [nvarchar](420) NULL,
	[toggle] [bit] NULL,
	[value] [nvarchar](180) NOT NULL,
	[min] [nvarchar](40) NULL,
	[max] [nvarchar](40) NULL,
	[default_value] [nvarchar](180) NULL,
	[updated_by] [nvarchar](220) NOT NULL,
	[create_ts] [datetime] NOT NULL,
	
PRIMARY KEY  
(
	[id] ASC
)) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

ALTER TABLE [dbo].[user_preferences] ADD CONSTRAINT [DF_user_preferences_create_ts]  DEFAULT (getdate()) FOR [create_ts]
GO

CREATE INDEX airline1 ON user_preferences (airline);
GO
CREATE UNIQUE INDEX userkey1 ON user_preferences (airline, user_key);
GO







/****** Object:  Table [dbo].[war_releases]    Script Date: 9/5/2019 12:40:24 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[war_releases](
	[supa_release] [nvarchar](64) NOT NULL,
	[supa_part_number] [nvarchar](64) NOT NULL,
	[path] [nvarchar](128) NOT NULL,
	[airline] [nvarchar](64) NOT NULL,
	[create_ts] [datetimeoffset](7) NOT NULL,
	[update_ts] [datetimeoffset](7) NULL,
 CONSTRAINT [PK_war_releases] PRIMARY KEY CLUSTERED 
(
	[supa_release] ASC,
	[airline] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[war_releases] ADD  CONSTRAINT [DF_war_releases_CreateTS]  DEFAULT (sysutcdatetime()) FOR [create_ts]
GO


CREATE UNIQUE NONCLUSTERED INDEX [UNIQ_war_releases_supa_part_number_airline] ON [dbo].[war_releases]
(
	[supa_part_number] ASC,
	[airline] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

CREATE TRIGGER [dbo].[WarReleases_Trigger_UpdateTS]
ON [dbo].[war_releases]
AFTER INSERT, UPDATE
AS BEGIN

       SET NOCOUNT ON;

       UPDATE SR
              SET SR.update_ts = SYSUTCDATETIME()
       FROM war_releases AS SR
              INNER JOIN inserted AS I
                     ON SR.supa_release = I.supa_release
                        AND SR.airline = I.airline;

END
GO

ALTER TABLE [dbo].[war_releases] ENABLE TRIGGER [WarReleases_Trigger_UpdateTS]
GO



CREATE TRIGGER [dbo].[WarReleases_Trigger_DeleteEntry] 
ON [dbo].[war_releases]
FOR DELETE
AS BEGIN
	SET NOCOUNT ON;

	Delete FROM [dbo].[current_supa_releases] 
	WHERE Exists (SELECT 1 
              FROM deleted del
              WHERE del.airline = current_supa_releases.airline 
			  AND del.supa_release = current_supa_releases.release)             

END

ALTER TABLE [dbo].[war_releases] ENABLE TRIGGER [WarReleases_Trigger_DeleteEntry]
GO



-------

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[current_supa_releases](
	[id] [int] NOT NULL IDENTITY(1,1),
	[airline] [nvarchar](50) NOT NULL,
	[release] [nvarchar](220) NOT NULL,
	[description] [text] NOT NULL,
	[release_date] [datetime] NOT NULL,
	[updated_by] [nvarchar](220) NOT NULL,
	[create_ts] [datetime] NOT NULL,
	
PRIMARY KEY  
(
	[id] ASC
)) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

ALTER TABLE [dbo].[current_supa_releases] ADD CONSTRAINT [DF_current_supa_releases_create_ts]  DEFAULT (getdate()) FOR [create_ts]
GO

ALTER TABLE [dbo].[current_supa_releases] ADD CONSTRAINT [DF_current_supa_releases_release_date]  DEFAULT (getdate()) FOR [release_date]
GO

CREATE INDEX supa_airline1 ON current_supa_releases (airline);
GO
CREATE UNIQUE INDEX supakey1 ON current_supa_releases (airline, release);
GO

