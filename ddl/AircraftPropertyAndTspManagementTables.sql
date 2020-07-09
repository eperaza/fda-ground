IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Platform')
BEGIN
	CREATE TABLE [dbo].[Platform](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[Name] [varchar](50) NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
		CONSTRAINT [UC_Platform_Name] UNIQUE([Name]),
	 CONSTRAINT [PK_Platform] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Property')
BEGIN
	CREATE TABLE [dbo].[Property](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[Name] [varchar](50) NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
		CONSTRAINT [UC_Property_Name] UNIQUE([Name]),
	 CONSTRAINT [PK_Property] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'PlatformProperty')
BEGIN
	CREATE TABLE [dbo].[PlatformProperty](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[PlatformID] [int] NOT NULL,
		[PropertyID] [int] NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
	 CONSTRAINT [PK_PlatformProperty] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[PlatformProperty]  WITH CHECK ADD CONSTRAINT [FK_PlatformProperty_Platform] FOREIGN KEY([PlatformID])
	REFERENCES [dbo].[Platform] ([ID])
	ON DELETE CASCADE
	
	ALTER TABLE [dbo].[PlatformProperty]  WITH CHECK ADD CONSTRAINT [FK_PlatformProperty_Property] FOREIGN KEY([PropertyID])
	REFERENCES [dbo].[Property] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[PlatformProperty] CHECK CONSTRAINT [FK_PlatformProperty_Platform]

	ALTER TABLE [dbo].[PlatformProperty] CHECK CONSTRAINT [FK_PlatformProperty_Property]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'PropertyValue')
BEGIN
	CREATE TABLE [dbo].[PropertyValue](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[PlatformPropertyID] [int] NOT NULL,
		[Value] [varchar] (100) NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
	 CONSTRAINT [PK_PropertyValue] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[PropertyValue]  WITH CHECK ADD CONSTRAINT [FK_PropertyValue_PlatformProperty] FOREIGN KEY([PlatformPropertyID])
	REFERENCES [dbo].[PlatformProperty] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[PropertyValue] CHECK CONSTRAINT [FK_PropertyValue_PlatformProperty]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'AircraftType')
BEGIN
	CREATE TABLE [dbo].[AircraftType](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[Name] [varchar](50) NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
		CONSTRAINT [UC_AircraftType_Name] UNIQUE([Name]),
	 CONSTRAINT [PK_AircraftType] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Airline')
BEGIN
	CREATE TABLE [dbo].[Airline](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[Name] [varchar](50) NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
		CONSTRAINT [UC_Airline_Name] UNIQUE([Name]),
	 CONSTRAINT [PK_Airline] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'AircraftInfo')
BEGIN
	CREATE TABLE [dbo].[AircraftInfo](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[AirlineID] [int] NOT NULL,
		[AircraftTypeID] [int] NOT NULL,
		[TailNumber] [nvarchar](20) NOT NULL,
		[Active] [bit] NOT NULL DEFAULT (1),
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
		CONSTRAINT [UC_AircraftInfo_AirlineId_TailNumber] UNIQUE([AirlineID], [TailNumber]),
	 CONSTRAINT [PK_AircraftInfo] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[AircraftInfo]  WITH CHECK ADD CONSTRAINT [FK_AircraftInfo_Airline] FOREIGN KEY([AirlineID])
	REFERENCES [dbo].[Airline] ([ID])
	ON DELETE CASCADE
	
	ALTER TABLE [dbo].[AircraftInfo]  WITH CHECK ADD CONSTRAINT [FK_AircraftInfo_AircraftType] FOREIGN KEY([AircraftTypeID])
	REFERENCES [dbo].[AircraftType] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[AircraftInfo] CHECK CONSTRAINT [FK_AircraftInfo_Airline]

	ALTER TABLE [dbo].[AircraftInfo] CHECK CONSTRAINT [FK_AircraftInfo_AircraftType]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'AircraftProperty')
BEGIN
	CREATE TABLE [dbo].[AircraftProperty](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[AircraftInfoID] [int] NOT NULL,
		[PropertyValueID] [int] NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
	 CONSTRAINT [PK_AircraftProperty] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[AircraftProperty]  WITH CHECK ADD  CONSTRAINT [FK_AircraftProperty_AircraftInfo] FOREIGN KEY([AircraftInfoID])
	REFERENCES [dbo].[AircraftInfo] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[AircraftProperty]  WITH CHECK ADD  CONSTRAINT [FK_AircraftProperty_PropertyValue] FOREIGN KEY([PropertyValueID])
	REFERENCES [dbo].[PropertyValue] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[AircraftProperty] CHECK CONSTRAINT [FK_AircraftProperty_AircraftInfo]
	
	ALTER TABLE [dbo].[AircraftProperty] CHECK CONSTRAINT [FK_AircraftProperty_PropertyValue]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TSP')
BEGIN
	CREATE TABLE [dbo].[TSP](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[AircraftInfoID] [int] NOT NULL,
		[TspContent] [varchar](8000) NOT NULL,
		[Version] [varchar](10) NOT NULL,
		[CutoffDate] [datetime] NULL,
		[NumberOfFlights] [int] NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
		CONSTRAINT [UC_TSP_AircraftInfoID_Version] UNIQUE([AircraftInfoID], [Version]),
	 CONSTRAINT [PK_TSP] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[TSP]  WITH CHECK ADD  CONSTRAINT [FK_TSP_AircraftInfo] FOREIGN KEY([AircraftInfoID])
	REFERENCES [dbo].[AircraftInfo] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[TSP] CHECK CONSTRAINT [FK_TSP_AircraftInfo]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'ActiveTSP')
BEGIN
	CREATE TABLE [dbo].[ActiveTSP](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[TspID] [int] NOT NULL,
		[AircraftInfoID] int NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
		CONSTRAINT [UC_ActiveTSP_AircraftInfoID] UNIQUE([AircraftInfoID]),
	 CONSTRAINT [PK_ActiveTSP] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[ActiveTSP]  WITH CHECK ADD  CONSTRAINT [FK_ActiveTSP_TSP] FOREIGN KEY([TspID])
	REFERENCES [dbo].[TSP] ([ID])
	ON DELETE CASCADE
	
	ALTER TABLE [dbo].[ActiveTSP]  WITH CHECK ADD  CONSTRAINT [FK_ActiveTSP_AircraftInfo] FOREIGN KEY([AircraftInfoID])
	REFERENCES [dbo].[AircraftInfo] ([ID])
	ON DELETE NO ACTION

	ALTER TABLE [dbo].[ActiveTSP] CHECK CONSTRAINT [FK_ActiveTSP_TSP]
	
	ALTER TABLE [dbo].[ActiveTSP] CHECK CONSTRAINT [FK_ActiveTSP_AircraftInfo]
END