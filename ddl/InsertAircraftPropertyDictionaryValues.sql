USE [FDAGroundServices]
GO

-- insert into AircraftType table
INSERT INTO [dbo].[AircraftType] (Name)
VALUES ('B737-600'), ('B737-600DAC'), ('B737-600W'),
	   ('B737-600WDAC'), ('B737-700'), ('B737-700DAC'),
	   ('B737-700W'), ('B737-700WDAC'), ('B737-700BBJ'),
	   ('B737-700BBJW'), ('B737-800'), ('B737-800DAC'),
	   ('B737-800W'), ('B737-800WDAC'), ('B737-900'),
	   ('B737-900DAC'), ('B737-900W'), ('B737-900WDAC'),
	   ('B737-900ER'), ('B737-900ERW'), ('737MAX-7'),
	   ('737MAX-8'), ('737MAX-9'), ('737MAX-BBJ'),
	   ('787-8'), ('787-9'), ('787-10');

-- insert into Platform table
INSERT INTO [dbo].[Platform] (Name)
VALUES ('webFB'), ('ONS'), ('TPED');

-- insert into Property table
INSERT INTO [dbo].[Property] (Name)
VALUES ('aircraft.717format'), ('ons.dataType');

-- insert into PlatformProperty table
INSERT INTO [dbo].[PlatformProperty] (PlatformID, PropertyID)
SELECT platform.ID, property.ID FROM dbo.Platform platform, dbo.Property property
WHERE platform.Name = 'webFB' AND property.Name = 'aircraft.717format';

INSERT INTO [dbo].[PlatformProperty] (PlatformID, PropertyID)
SELECT platform.ID, property.ID FROM dbo.Platform platform, dbo.Property property
WHERE platform.Name IN ('ONS', 'TPED') AND property.Name = 'ons.dataType';

-- insert into PropertyValue table
INSERT INTO [dbo].[PropertyValue] (PlatformPropertyID, Value)
SELECT platformProperty.ID, appendices.Appendix 
FROM (VALUES ('AppendixA'), ('AppendixC'), ('AppendixD'), ('AppendixE')) as appendices(Appendix), 
dbo.PlatformProperty platformProperty WITH(NOLOCK)
JOIN dbo.Property property WITH(NOLOCK) ON property.ID = platformProperty.PropertyID
WHERE property.Name = 'aircraft.717format';

INSERT INTO [dbo].[PropertyValue] (PlatformPropertyID, Value)
SELECT platformProperty.ID, onsDataType.dataType 
FROM (VALUES ('a717'), ('param')) as onsDataType(dataType), 
dbo.PlatformProperty platformProperty WITH(NOLOCK)
JOIN dbo.Property property WITH(NOLOCK) ON property.ID = platformProperty.PropertyID
JOIN dbo.Platform platform WITH(NOLOCK) ON platform.ID = platformProperty.PlatformID
WHERE property.Name = 'ons.dataType' and platform.Name = 'ONS';

INSERT INTO [dbo].[PropertyValue] (PlatformPropertyID, Value)
SELECT platformProperty.ID, tpedDataType.dataType 
FROM (VALUES ('eafr')) as tpedDataType(dataType), 
dbo.PlatformProperty platformProperty WITH(NOLOCK)
JOIN dbo.Property property WITH(NOLOCK) ON property.ID = platformProperty.PropertyID
JOIN dbo.Platform platform WITH(NOLOCK) ON platform.ID = platformProperty.PlatformID
WHERE property.Name = 'ons.dataType' and platform.Name = 'TPED';
