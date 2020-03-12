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

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'AirlineTail')
BEGIN
	CREATE TABLE [dbo].[AirlineTail](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[AirlineID] [int] NOT NULL,
		[TailNumber] [nvarchar](20) NOT NULL,
		[Active] [bit] NOT NULL DEFAULT (1),
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
		CONSTRAINT [UC_AirlineTail_AirlineId_TailNumber] UNIQUE([AirlineID], [TailNumber]),
	 CONSTRAINT [PK_AirlineTail] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[AirlineTail]  WITH CHECK ADD CONSTRAINT [FK_AirlineTail_Airline] FOREIGN KEY([AirlineID])
	REFERENCES [dbo].[Airline] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[AirlineTail] CHECK CONSTRAINT [FK_AirlineTail_Airline]

END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TSP')
BEGIN
	CREATE TABLE [dbo].[TSP](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[AirlineTailID] [int] NOT NULL,
		[TspContent] [nvarchar](1000) NOT NULL,
		[Version] [varchar](10) NOT NULL,
		[EffectiveDate] [datetimeoffset] NULL,
		[Stage] [varchar](10) NOT NULL DEFAULT ('DEV'),
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
	 CONSTRAINT [PK_TSP] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[TSP]  WITH CHECK ADD  CONSTRAINT [FK_TSP_AirlineTail] FOREIGN KEY([AirlineTailID])
	REFERENCES [dbo].[AirlineTail] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[TSP] CHECK CONSTRAINT [FK_TSP_AirlineTail]
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'ActiveTSP')
BEGIN
	CREATE TABLE [dbo].[ActiveTSP](
		[ID] [int] IDENTITY(1,1) NOT NULL,
		[TspID] [int] NOT NULL,
		[CreatedDate] [datetime] NOT NULL DEFAULT GETDATE(),
		[CreatedBy] [nvarchar](50) NOT NULL DEFAULT ('SYSTEM'),
		[UpdatedDate] [datetime] NULL,
		[UpdatedBy] [nvarchar](50) NULL,
	 CONSTRAINT [PK_ActiveTSP] PRIMARY KEY CLUSTERED 
	(
		[ID] ASC
	)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
	) ON [PRIMARY]

	ALTER TABLE [dbo].[ActiveTSP]  WITH CHECK ADD  CONSTRAINT [FK_ActivTSP_TSP] FOREIGN KEY([TspID])
	REFERENCES [dbo].[TSP] ([ID])
	ON DELETE CASCADE

	ALTER TABLE [dbo].[ActiveTSP] CHECK CONSTRAINT [FK_ActivTSP_TSP]
END