// $ANTLR 3.5.2 FromClauseASTParser.g 2023-03-06 17:56:59

package org.apache.flink.table.planner.delegation.hive.parse;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.flink.table.planner.delegation.hive.copy.HiveParserASTNode;
import org.apache.flink.table.planner.delegation.hive.copy.HiveASTParseError;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.antlr.runtime.tree.*;


/** Counterpart of hive's FromClauseParser.g. */
@SuppressWarnings("all")
public class HiveASTParser_FromClauseASTParser extends Parser {
	public static final int EOF=-1;
	public static final int AMPERSAND=4;
	public static final int BITWISEOR=5;
	public static final int BITWISEXOR=6;
	public static final int ByteLengthLiteral=7;
	public static final int COLON=8;
	public static final int COMMA=9;
	public static final int CONCATENATE=10;
	public static final int CharSetLiteral=11;
	public static final int CharSetName=12;
	public static final int DIV=13;
	public static final int DIVIDE=14;
	public static final int DOLLAR=15;
	public static final int DOT=16;
	public static final int Digit=17;
	public static final int EQUAL=18;
	public static final int EQUAL_NS=19;
	public static final int Exponent=20;
	public static final int GREATERTHAN=21;
	public static final int GREATERTHANOREQUALTO=22;
	public static final int HexDigit=23;
	public static final int Identifier=24;
	public static final int IntegralLiteral=25;
	public static final int KW_ABORT=26;
	public static final int KW_ADD=27;
	public static final int KW_ADMIN=28;
	public static final int KW_AFTER=29;
	public static final int KW_ALL=30;
	public static final int KW_ALTER=31;
	public static final int KW_ANALYZE=32;
	public static final int KW_AND=33;
	public static final int KW_ARCHIVE=34;
	public static final int KW_ARRAY=35;
	public static final int KW_AS=36;
	public static final int KW_ASC=37;
	public static final int KW_AUTHORIZATION=38;
	public static final int KW_AUTOCOMMIT=39;
	public static final int KW_BEFORE=40;
	public static final int KW_BETWEEN=41;
	public static final int KW_BIGINT=42;
	public static final int KW_BINARY=43;
	public static final int KW_BOOLEAN=44;
	public static final int KW_BOTH=45;
	public static final int KW_BUCKET=46;
	public static final int KW_BUCKETS=47;
	public static final int KW_BY=48;
	public static final int KW_CACHE=49;
	public static final int KW_CASCADE=50;
	public static final int KW_CASE=51;
	public static final int KW_CAST=52;
	public static final int KW_CHANGE=53;
	public static final int KW_CHAR=54;
	public static final int KW_CLUSTER=55;
	public static final int KW_CLUSTERED=56;
	public static final int KW_CLUSTERSTATUS=57;
	public static final int KW_COLLECTION=58;
	public static final int KW_COLUMN=59;
	public static final int KW_COLUMNS=60;
	public static final int KW_COMMENT=61;
	public static final int KW_COMMIT=62;
	public static final int KW_COMPACT=63;
	public static final int KW_COMPACTIONS=64;
	public static final int KW_COMPUTE=65;
	public static final int KW_CONCATENATE=66;
	public static final int KW_CONF=67;
	public static final int KW_CONSTRAINT=68;
	public static final int KW_CONTINUE=69;
	public static final int KW_CREATE=70;
	public static final int KW_CROSS=71;
	public static final int KW_CUBE=72;
	public static final int KW_CURRENT=73;
	public static final int KW_CURRENT_DATE=74;
	public static final int KW_CURRENT_TIMESTAMP=75;
	public static final int KW_CURSOR=76;
	public static final int KW_DATA=77;
	public static final int KW_DATABASE=78;
	public static final int KW_DATABASES=79;
	public static final int KW_DATE=80;
	public static final int KW_DATETIME=81;
	public static final int KW_DAY=82;
	public static final int KW_DBPROPERTIES=83;
	public static final int KW_DECIMAL=84;
	public static final int KW_DEFERRED=85;
	public static final int KW_DEFINED=86;
	public static final int KW_DELETE=87;
	public static final int KW_DELIMITED=88;
	public static final int KW_DEPENDENCY=89;
	public static final int KW_DESC=90;
	public static final int KW_DESCRIBE=91;
	public static final int KW_DETAIL=92;
	public static final int KW_DIRECTORIES=93;
	public static final int KW_DIRECTORY=94;
	public static final int KW_DISABLE=95;
	public static final int KW_DISTINCT=96;
	public static final int KW_DISTRIBUTE=97;
	public static final int KW_DOUBLE=98;
	public static final int KW_DOW=99;
	public static final int KW_DROP=100;
	public static final int KW_DUMP=101;
	public static final int KW_ELEM_TYPE=102;
	public static final int KW_ELSE=103;
	public static final int KW_ENABLE=104;
	public static final int KW_END=105;
	public static final int KW_ESCAPED=106;
	public static final int KW_EXCEPT=107;
	public static final int KW_EXCHANGE=108;
	public static final int KW_EXCLUSIVE=109;
	public static final int KW_EXISTS=110;
	public static final int KW_EXPLAIN=111;
	public static final int KW_EXPORT=112;
	public static final int KW_EXPRESSION=113;
	public static final int KW_EXTENDED=114;
	public static final int KW_EXTERNAL=115;
	public static final int KW_EXTRACT=116;
	public static final int KW_FALSE=117;
	public static final int KW_FETCH=118;
	public static final int KW_FIELDS=119;
	public static final int KW_FILE=120;
	public static final int KW_FILEFORMAT=121;
	public static final int KW_FIRST=122;
	public static final int KW_FLOAT=123;
	public static final int KW_FLOOR=124;
	public static final int KW_FOLLOWING=125;
	public static final int KW_FOR=126;
	public static final int KW_FOREIGN=127;
	public static final int KW_FORMAT=128;
	public static final int KW_FORMATTED=129;
	public static final int KW_FROM=130;
	public static final int KW_FULL=131;
	public static final int KW_FUNCTION=132;
	public static final int KW_FUNCTIONS=133;
	public static final int KW_GRANT=134;
	public static final int KW_GROUP=135;
	public static final int KW_GROUPING=136;
	public static final int KW_HAVING=137;
	public static final int KW_HOUR=138;
	public static final int KW_IDXPROPERTIES=139;
	public static final int KW_IF=140;
	public static final int KW_IMPORT=141;
	public static final int KW_IN=142;
	public static final int KW_INDEX=143;
	public static final int KW_INDEXES=144;
	public static final int KW_INNER=145;
	public static final int KW_INPATH=146;
	public static final int KW_INPUTDRIVER=147;
	public static final int KW_INPUTFORMAT=148;
	public static final int KW_INSERT=149;
	public static final int KW_INT=150;
	public static final int KW_INTERSECT=151;
	public static final int KW_INTERVAL=152;
	public static final int KW_INTO=153;
	public static final int KW_IS=154;
	public static final int KW_ISOLATION=155;
	public static final int KW_ITEMS=156;
	public static final int KW_JAR=157;
	public static final int KW_JOIN=158;
	public static final int KW_KEY=159;
	public static final int KW_KEYS=160;
	public static final int KW_KEY_TYPE=161;
	public static final int KW_LAST=162;
	public static final int KW_LATERAL=163;
	public static final int KW_LEFT=164;
	public static final int KW_LESS=165;
	public static final int KW_LEVEL=166;
	public static final int KW_LIKE=167;
	public static final int KW_LIMIT=168;
	public static final int KW_LINES=169;
	public static final int KW_LOAD=170;
	public static final int KW_LOCAL=171;
	public static final int KW_LOCATION=172;
	public static final int KW_LOCK=173;
	public static final int KW_LOCKS=174;
	public static final int KW_LOGICAL=175;
	public static final int KW_LONG=176;
	public static final int KW_MACRO=177;
	public static final int KW_MAP=178;
	public static final int KW_MAPJOIN=179;
	public static final int KW_MATCHED=180;
	public static final int KW_MATERIALIZED=181;
	public static final int KW_MERGE=182;
	public static final int KW_METADATA=183;
	public static final int KW_MINUS=184;
	public static final int KW_MINUTE=185;
	public static final int KW_MONTH=186;
	public static final int KW_MORE=187;
	public static final int KW_MSCK=188;
	public static final int KW_NONE=189;
	public static final int KW_NORELY=190;
	public static final int KW_NOSCAN=191;
	public static final int KW_NOT=192;
	public static final int KW_NOVALIDATE=193;
	public static final int KW_NULL=194;
	public static final int KW_NULLS=195;
	public static final int KW_OF=196;
	public static final int KW_OFFSET=197;
	public static final int KW_ON=198;
	public static final int KW_ONLY=199;
	public static final int KW_OPERATOR=200;
	public static final int KW_OPTION=201;
	public static final int KW_OR=202;
	public static final int KW_ORDER=203;
	public static final int KW_OUT=204;
	public static final int KW_OUTER=205;
	public static final int KW_OUTPUTDRIVER=206;
	public static final int KW_OUTPUTFORMAT=207;
	public static final int KW_OVER=208;
	public static final int KW_OVERWRITE=209;
	public static final int KW_OWNER=210;
	public static final int KW_PARTIALSCAN=211;
	public static final int KW_PARTITION=212;
	public static final int KW_PARTITIONED=213;
	public static final int KW_PARTITIONS=214;
	public static final int KW_PERCENT=215;
	public static final int KW_PLUS=216;
	public static final int KW_PRECEDING=217;
	public static final int KW_PRECISION=218;
	public static final int KW_PRESERVE=219;
	public static final int KW_PRETTY=220;
	public static final int KW_PRIMARY=221;
	public static final int KW_PRINCIPALS=222;
	public static final int KW_PROCEDURE=223;
	public static final int KW_PURGE=224;
	public static final int KW_QUARTER=225;
	public static final int KW_RANGE=226;
	public static final int KW_READ=227;
	public static final int KW_READS=228;
	public static final int KW_REBUILD=229;
	public static final int KW_RECORDREADER=230;
	public static final int KW_RECORDWRITER=231;
	public static final int KW_REDUCE=232;
	public static final int KW_REFERENCES=233;
	public static final int KW_REGEXP=234;
	public static final int KW_RELOAD=235;
	public static final int KW_RELY=236;
	public static final int KW_RENAME=237;
	public static final int KW_REPAIR=238;
	public static final int KW_REPL=239;
	public static final int KW_REPLACE=240;
	public static final int KW_REPLICATION=241;
	public static final int KW_RESTRICT=242;
	public static final int KW_REVOKE=243;
	public static final int KW_REWRITE=244;
	public static final int KW_RIGHT=245;
	public static final int KW_RLIKE=246;
	public static final int KW_ROLE=247;
	public static final int KW_ROLES=248;
	public static final int KW_ROLLBACK=249;
	public static final int KW_ROLLUP=250;
	public static final int KW_ROW=251;
	public static final int KW_ROWS=252;
	public static final int KW_SCHEMA=253;
	public static final int KW_SCHEMAS=254;
	public static final int KW_SECOND=255;
	public static final int KW_SELECT=256;
	public static final int KW_SEMI=257;
	public static final int KW_SERDE=258;
	public static final int KW_SERDEPROPERTIES=259;
	public static final int KW_SERVER=260;
	public static final int KW_SET=261;
	public static final int KW_SETS=262;
	public static final int KW_SHARED=263;
	public static final int KW_SHOW=264;
	public static final int KW_SHOW_DATABASE=265;
	public static final int KW_SKEWED=266;
	public static final int KW_SMALLINT=267;
	public static final int KW_SNAPSHOT=268;
	public static final int KW_SORT=269;
	public static final int KW_SORTED=270;
	public static final int KW_SSL=271;
	public static final int KW_START=272;
	public static final int KW_STATISTICS=273;
	public static final int KW_STATUS=274;
	public static final int KW_STORED=275;
	public static final int KW_STREAMTABLE=276;
	public static final int KW_STRING=277;
	public static final int KW_STRUCT=278;
	public static final int KW_SUMMARY=279;
	public static final int KW_TABLE=280;
	public static final int KW_TABLES=281;
	public static final int KW_TABLESAMPLE=282;
	public static final int KW_TBLPROPERTIES=283;
	public static final int KW_TEMPORARY=284;
	public static final int KW_TERMINATED=285;
	public static final int KW_THEN=286;
	public static final int KW_TIMESTAMP=287;
	public static final int KW_TINYINT=288;
	public static final int KW_TO=289;
	public static final int KW_TOUCH=290;
	public static final int KW_TRANSACTION=291;
	public static final int KW_TRANSACTIONS=292;
	public static final int KW_TRANSFORM=293;
	public static final int KW_TRIGGER=294;
	public static final int KW_TRUE=295;
	public static final int KW_TRUNCATE=296;
	public static final int KW_UNARCHIVE=297;
	public static final int KW_UNBOUNDED=298;
	public static final int KW_UNDO=299;
	public static final int KW_UNION=300;
	public static final int KW_UNIONTYPE=301;
	public static final int KW_UNIQUE=302;
	public static final int KW_UNIQUEJOIN=303;
	public static final int KW_UNLOCK=304;
	public static final int KW_UNSET=305;
	public static final int KW_UNSIGNED=306;
	public static final int KW_UPDATE=307;
	public static final int KW_URI=308;
	public static final int KW_USE=309;
	public static final int KW_USER=310;
	public static final int KW_USING=311;
	public static final int KW_UTC=312;
	public static final int KW_UTCTIMESTAMP=313;
	public static final int KW_VALIDATE=314;
	public static final int KW_VALUES=315;
	public static final int KW_VALUE_TYPE=316;
	public static final int KW_VARCHAR=317;
	public static final int KW_VECTORIZATION=318;
	public static final int KW_VIEW=319;
	public static final int KW_VIEWS=320;
	public static final int KW_WAIT=321;
	public static final int KW_WEEK=322;
	public static final int KW_WHEN=323;
	public static final int KW_WHERE=324;
	public static final int KW_WHILE=325;
	public static final int KW_WINDOW=326;
	public static final int KW_WITH=327;
	public static final int KW_WORK=328;
	public static final int KW_WRITE=329;
	public static final int KW_YEAR=330;
	public static final int LCURLY=331;
	public static final int LESSTHAN=332;
	public static final int LESSTHANOREQUALTO=333;
	public static final int LINE_COMMENT=334;
	public static final int LPAREN=335;
	public static final int LSQUARE=336;
	public static final int Letter=337;
	public static final int MINUS=338;
	public static final int MOD=339;
	public static final int NOTEQUAL=340;
	public static final int Number=341;
	public static final int NumberLiteral=342;
	public static final int PLUS=343;
	public static final int QUERY_HINT=344;
	public static final int QUESTION=345;
	public static final int QuotedIdentifier=346;
	public static final int RCURLY=347;
	public static final int RPAREN=348;
	public static final int RSQUARE=349;
	public static final int RegexComponent=350;
	public static final int SEMICOLON=351;
	public static final int STAR=352;
	public static final int StringLiteral=353;
	public static final int TILDE=354;
	public static final int WS=355;
	public static final int KW_BATCH=385;
	public static final int KW_DAYOFWEEK=420;
	public static final int KW_HOLD_DDLTIME=463;
	public static final int KW_IGNORE=467;
	public static final int KW_NO_DROP=510;
	public static final int KW_OFFLINE=514;
	public static final int KW_PROTECTION=537;
	public static final int KW_READONLY=542;
	public static final int TOK_ABORT_TRANSACTIONS=649;
	public static final int TOK_ADMIN_OPTION_FOR=650;
	public static final int TOK_ALIASLIST=651;
	public static final int TOK_ALLCOLREF=652;
	public static final int TOK_ALTERDATABASE_LOCATION=653;
	public static final int TOK_ALTERDATABASE_OWNER=654;
	public static final int TOK_ALTERDATABASE_PROPERTIES=655;
	public static final int TOK_ALTERINDEX_PROPERTIES=656;
	public static final int TOK_ALTERINDEX_REBUILD=657;
	public static final int TOK_ALTERTABLE=658;
	public static final int TOK_ALTERTABLE_ADDCOLS=659;
	public static final int TOK_ALTERTABLE_ADDCONSTRAINT=660;
	public static final int TOK_ALTERTABLE_ADDPARTS=661;
	public static final int TOK_ALTERTABLE_ARCHIVE=662;
	public static final int TOK_ALTERTABLE_BUCKETS=663;
	public static final int TOK_ALTERTABLE_CHANGECOL_AFTER_POSITION=664;
	public static final int TOK_ALTERTABLE_CLUSTER_SORT=665;
	public static final int TOK_ALTERTABLE_COMPACT=666;
	public static final int TOK_ALTERTABLE_DROPCONSTRAINT=667;
	public static final int TOK_ALTERTABLE_DROPPARTS=668;
	public static final int TOK_ALTERTABLE_DROPPROPERTIES=669;
	public static final int TOK_ALTERTABLE_EXCHANGEPARTITION=670;
	public static final int TOK_ALTERTABLE_FILEFORMAT=671;
	public static final int TOK_ALTERTABLE_LOCATION=672;
	public static final int TOK_ALTERTABLE_MERGEFILES=673;
	public static final int TOK_ALTERTABLE_PARTCOLTYPE=674;
	public static final int TOK_ALTERTABLE_PROPERTIES=675;
	public static final int TOK_ALTERTABLE_RENAME=676;
	public static final int TOK_ALTERTABLE_RENAMECOL=677;
	public static final int TOK_ALTERTABLE_RENAMEPART=678;
	public static final int TOK_ALTERTABLE_REPLACECOLS=679;
	public static final int TOK_ALTERTABLE_SERDEPROPERTIES=680;
	public static final int TOK_ALTERTABLE_SERIALIZER=681;
	public static final int TOK_ALTERTABLE_SKEWED=682;
	public static final int TOK_ALTERTABLE_SKEWED_LOCATION=683;
	public static final int TOK_ALTERTABLE_TOUCH=684;
	public static final int TOK_ALTERTABLE_UNARCHIVE=685;
	public static final int TOK_ALTERTABLE_UPDATECOLSTATS=686;
	public static final int TOK_ALTERTABLE_UPDATESTATS=687;
	public static final int TOK_ALTERVIEW=688;
	public static final int TOK_ALTERVIEW_ADDPARTS=689;
	public static final int TOK_ALTERVIEW_DROPPARTS=690;
	public static final int TOK_ALTERVIEW_DROPPROPERTIES=691;
	public static final int TOK_ALTERVIEW_PROPERTIES=692;
	public static final int TOK_ALTERVIEW_RENAME=693;
	public static final int TOK_ANALYZE=694;
	public static final int TOK_ANONYMOUS=695;
	public static final int TOK_ARCHIVE=696;
	public static final int TOK_BIGINT=697;
	public static final int TOK_BINARY=698;
	public static final int TOK_BLOCKING=699;
	public static final int TOK_BOOLEAN=700;
	public static final int TOK_CACHE_METADATA=701;
	public static final int TOK_CASCADE=702;
	public static final int TOK_CHAR=703;
	public static final int TOK_CHARSETLITERAL=704;
	public static final int TOK_CLUSTERBY=705;
	public static final int TOK_COLTYPELIST=706;
	public static final int TOK_COL_NAME=707;
	public static final int TOK_COMMIT=708;
	public static final int TOK_CONSTRAINT_NAME=709;
	public static final int TOK_CREATEDATABASE=710;
	public static final int TOK_CREATEFUNCTION=711;
	public static final int TOK_CREATEINDEX=712;
	public static final int TOK_CREATEINDEX_INDEXTBLNAME=713;
	public static final int TOK_CREATEMACRO=714;
	public static final int TOK_CREATEROLE=715;
	public static final int TOK_CREATETABLE=716;
	public static final int TOK_CREATEVIEW=717;
	public static final int TOK_CREATE_MATERIALIZED_VIEW=718;
	public static final int TOK_CROSSJOIN=719;
	public static final int TOK_CTE=720;
	public static final int TOK_CUBE_GROUPBY=721;
	public static final int TOK_DATABASECOMMENT=722;
	public static final int TOK_DATABASELOCATION=723;
	public static final int TOK_DATABASEPROPERTIES=724;
	public static final int TOK_DATE=725;
	public static final int TOK_DATELITERAL=726;
	public static final int TOK_DATETIME=727;
	public static final int TOK_DBPROPLIST=728;
	public static final int TOK_DB_TYPE=729;
	public static final int TOK_DECIMAL=730;
	public static final int TOK_DEFERRED_REBUILDINDEX=731;
	public static final int TOK_DELETE=732;
	public static final int TOK_DELETE_FROM=733;
	public static final int TOK_DESCDATABASE=734;
	public static final int TOK_DESCFUNCTION=735;
	public static final int TOK_DESCTABLE=736;
	public static final int TOK_DESTINATION=737;
	public static final int TOK_DETAIL=738;
	public static final int TOK_DIR=739;
	public static final int TOK_DISABLE=740;
	public static final int TOK_DISTRIBUTEBY=741;
	public static final int TOK_DOUBLE=742;
	public static final int TOK_DROPDATABASE=743;
	public static final int TOK_DROPFUNCTION=744;
	public static final int TOK_DROPINDEX=745;
	public static final int TOK_DROPMACRO=746;
	public static final int TOK_DROPROLE=747;
	public static final int TOK_DROPTABLE=748;
	public static final int TOK_DROPVIEW=749;
	public static final int TOK_DROP_MATERIALIZED_VIEW=750;
	public static final int TOK_ENABLE=751;
	public static final int TOK_EXCEPTALL=752;
	public static final int TOK_EXCEPTDISTINCT=753;
	public static final int TOK_EXPLAIN=754;
	public static final int TOK_EXPLAIN_SQ_REWRITE=755;
	public static final int TOK_EXPLIST=756;
	public static final int TOK_EXPORT=757;
	public static final int TOK_EXPRESSION=758;
	public static final int TOK_FALSE=759;
	public static final int TOK_FILE=760;
	public static final int TOK_FILEFORMAT_GENERIC=761;
	public static final int TOK_FLOAT=762;
	public static final int TOK_FOREIGN_KEY=763;
	public static final int TOK_FROM=764;
	public static final int TOK_FULLOUTERJOIN=765;
	public static final int TOK_FUNCTION=766;
	public static final int TOK_FUNCTIONDI=767;
	public static final int TOK_FUNCTIONSTAR=768;
	public static final int TOK_GRANT=769;
	public static final int TOK_GRANT_OPTION_FOR=770;
	public static final int TOK_GRANT_ROLE=771;
	public static final int TOK_GRANT_WITH_ADMIN_OPTION=772;
	public static final int TOK_GRANT_WITH_OPTION=773;
	public static final int TOK_GROUP=774;
	public static final int TOK_GROUPBY=775;
	public static final int TOK_GROUPING_SETS=776;
	public static final int TOK_GROUPING_SETS_EXPRESSION=777;
	public static final int TOK_HAVING=778;
	public static final int TOK_IFEXISTS=779;
	public static final int TOK_IFNOTEXISTS=780;
	public static final int TOK_IMPORT=781;
	public static final int TOK_INDEXCOMMENT=782;
	public static final int TOK_INDEXPROPERTIES=783;
	public static final int TOK_INDEXPROPLIST=784;
	public static final int TOK_INSERT=785;
	public static final int TOK_INSERT_INTO=786;
	public static final int TOK_INT=787;
	public static final int TOK_INTERSECTALL=788;
	public static final int TOK_INTERSECTDISTINCT=789;
	public static final int TOK_INTERVAL_DAY_LITERAL=790;
	public static final int TOK_INTERVAL_DAY_TIME=791;
	public static final int TOK_INTERVAL_DAY_TIME_LITERAL=792;
	public static final int TOK_INTERVAL_HOUR_LITERAL=793;
	public static final int TOK_INTERVAL_MINUTE_LITERAL=794;
	public static final int TOK_INTERVAL_MONTH_LITERAL=795;
	public static final int TOK_INTERVAL_SECOND_LITERAL=796;
	public static final int TOK_INTERVAL_YEAR_LITERAL=797;
	public static final int TOK_INTERVAL_YEAR_MONTH=798;
	public static final int TOK_INTERVAL_YEAR_MONTH_LITERAL=799;
	public static final int TOK_ISNOTNULL=800;
	public static final int TOK_ISNULL=801;
	public static final int TOK_ISOLATION_LEVEL=802;
	public static final int TOK_ISOLATION_SNAPSHOT=803;
	public static final int TOK_JAR=804;
	public static final int TOK_JOIN=805;
	public static final int TOK_LATERAL_VIEW=806;
	public static final int TOK_LATERAL_VIEW_OUTER=807;
	public static final int TOK_LEFTOUTERJOIN=808;
	public static final int TOK_LEFTSEMIJOIN=809;
	public static final int TOK_LENGTH=810;
	public static final int TOK_LIKETABLE=811;
	public static final int TOK_LIMIT=812;
	public static final int TOK_LIST=813;
	public static final int TOK_LOAD=814;
	public static final int TOK_LOCKDB=815;
	public static final int TOK_LOCKTABLE=816;
	public static final int TOK_MAP=817;
	public static final int TOK_MATCHED=818;
	public static final int TOK_MERGE=819;
	public static final int TOK_METADATA=820;
	public static final int TOK_MSCK=821;
	public static final int TOK_NORELY=822;
	public static final int TOK_NOT_CLUSTERED=823;
	public static final int TOK_NOT_MATCHED=824;
	public static final int TOK_NOT_NULL=825;
	public static final int TOK_NOT_SORTED=826;
	public static final int TOK_NOVALIDATE=827;
	public static final int TOK_NO_DROP=828;
	public static final int TOK_NULL=829;
	public static final int TOK_NULLS_FIRST=830;
	public static final int TOK_NULLS_LAST=831;
	public static final int TOK_OFFLINE=832;
	public static final int TOK_OFFSET=833;
	public static final int TOK_ONLY=834;
	public static final int TOK_OPERATOR=835;
	public static final int TOK_OP_ADD=836;
	public static final int TOK_OP_AND=837;
	public static final int TOK_OP_BITAND=838;
	public static final int TOK_OP_BITNOT=839;
	public static final int TOK_OP_BITOR=840;
	public static final int TOK_OP_BITXOR=841;
	public static final int TOK_OP_DIV=842;
	public static final int TOK_OP_EQ=843;
	public static final int TOK_OP_GE=844;
	public static final int TOK_OP_GT=845;
	public static final int TOK_OP_LE=846;
	public static final int TOK_OP_LIKE=847;
	public static final int TOK_OP_LT=848;
	public static final int TOK_OP_MOD=849;
	public static final int TOK_OP_MUL=850;
	public static final int TOK_OP_NE=851;
	public static final int TOK_OP_NOT=852;
	public static final int TOK_OP_OR=853;
	public static final int TOK_OP_SUB=854;
	public static final int TOK_ORDERBY=855;
	public static final int TOK_ORREPLACE=856;
	public static final int TOK_PARTITIONINGSPEC=857;
	public static final int TOK_PARTITIONLOCATION=858;
	public static final int TOK_PARTSPEC=859;
	public static final int TOK_PARTVAL=860;
	public static final int TOK_PERCENT=861;
	public static final int TOK_PRIMARY_KEY=862;
	public static final int TOK_PRINCIPAL_NAME=863;
	public static final int TOK_PRIVILEGE=864;
	public static final int TOK_PRIVILEGE_LIST=865;
	public static final int TOK_PRIV_ALL=866;
	public static final int TOK_PRIV_ALTER_DATA=867;
	public static final int TOK_PRIV_ALTER_METADATA=868;
	public static final int TOK_PRIV_CREATE=869;
	public static final int TOK_PRIV_DELETE=870;
	public static final int TOK_PRIV_DROP=871;
	public static final int TOK_PRIV_INDEX=872;
	public static final int TOK_PRIV_INSERT=873;
	public static final int TOK_PRIV_LOCK=874;
	public static final int TOK_PRIV_OBJECT=875;
	public static final int TOK_PRIV_OBJECT_COL=876;
	public static final int TOK_PRIV_SELECT=877;
	public static final int TOK_PRIV_SHOW_DATABASE=878;
	public static final int TOK_PTBLFUNCTION=879;
	public static final int TOK_QUERY=880;
	public static final int TOK_READONLY=881;
	public static final int TOK_RECORDREADER=882;
	public static final int TOK_RECORDWRITER=883;
	public static final int TOK_RELOADFUNCTION=884;
	public static final int TOK_RELY=885;
	public static final int TOK_REPLICATION=886;
	public static final int TOK_REPL_DUMP=887;
	public static final int TOK_REPL_LOAD=888;
	public static final int TOK_REPL_STATUS=889;
	public static final int TOK_RESOURCE_ALL=890;
	public static final int TOK_RESOURCE_LIST=891;
	public static final int TOK_RESOURCE_URI=892;
	public static final int TOK_RESTRICT=893;
	public static final int TOK_REVOKE=894;
	public static final int TOK_REVOKE_ROLE=895;
	public static final int TOK_REWRITE_DISABLED=896;
	public static final int TOK_REWRITE_ENABLED=897;
	public static final int TOK_RIGHTOUTERJOIN=898;
	public static final int TOK_ROLE=899;
	public static final int TOK_ROLLBACK=900;
	public static final int TOK_ROLLUP_GROUPBY=901;
	public static final int TOK_ROWCOUNT=902;
	public static final int TOK_SELECT=903;
	public static final int TOK_SELECTDI=904;
	public static final int TOK_SELEXPR=905;
	public static final int TOK_SERDE=906;
	public static final int TOK_SERDENAME=907;
	public static final int TOK_SERDEPROPS=908;
	public static final int TOK_SERVER_TYPE=909;
	public static final int TOK_SETCOLREF=910;
	public static final int TOK_SET_AUTOCOMMIT=911;
	public static final int TOK_SET_COLUMNS_CLAUSE=912;
	public static final int TOK_SHOWCOLUMNS=913;
	public static final int TOK_SHOWCONF=914;
	public static final int TOK_SHOWDATABASES=915;
	public static final int TOK_SHOWDBLOCKS=916;
	public static final int TOK_SHOWFUNCTIONS=917;
	public static final int TOK_SHOWINDEXES=918;
	public static final int TOK_SHOWLOCKS=919;
	public static final int TOK_SHOWPARTITIONS=920;
	public static final int TOK_SHOWTABLES=921;
	public static final int TOK_SHOWVIEWS=922;
	public static final int TOK_SHOW_COMPACTIONS=923;
	public static final int TOK_SHOW_CREATEDATABASE=924;
	public static final int TOK_SHOW_CREATETABLE=925;
	public static final int TOK_SHOW_GRANT=926;
	public static final int TOK_SHOW_ROLES=927;
	public static final int TOK_SHOW_ROLE_GRANT=928;
	public static final int TOK_SHOW_ROLE_PRINCIPALS=929;
	public static final int TOK_SHOW_SET_ROLE=930;
	public static final int TOK_SHOW_TABLESTATUS=931;
	public static final int TOK_SHOW_TBLPROPERTIES=932;
	public static final int TOK_SHOW_TRANSACTIONS=933;
	public static final int TOK_SKEWED_LOCATIONS=934;
	public static final int TOK_SKEWED_LOCATION_LIST=935;
	public static final int TOK_SKEWED_LOCATION_MAP=936;
	public static final int TOK_SMALLINT=937;
	public static final int TOK_SORTBY=938;
	public static final int TOK_START_TRANSACTION=939;
	public static final int TOK_STORAGEHANDLER=940;
	public static final int TOK_STOREDASDIRS=941;
	public static final int TOK_STRING=942;
	public static final int TOK_STRINGLITERALSEQUENCE=943;
	public static final int TOK_STRUCT=944;
	public static final int TOK_SUBQUERY=945;
	public static final int TOK_SUBQUERY_EXPR=946;
	public static final int TOK_SUBQUERY_OP=947;
	public static final int TOK_SUBQUERY_OP_NOTEXISTS=948;
	public static final int TOK_SUBQUERY_OP_NOTIN=949;
	public static final int TOK_SUMMARY=950;
	public static final int TOK_SWITCHDATABASE=951;
	public static final int TOK_TAB=952;
	public static final int TOK_TABALIAS=953;
	public static final int TOK_TABCOL=954;
	public static final int TOK_TABCOLLIST=955;
	public static final int TOK_TABCOLNAME=956;
	public static final int TOK_TABCOLVALUE=957;
	public static final int TOK_TABCOLVALUES=958;
	public static final int TOK_TABCOLVALUE_PAIR=959;
	public static final int TOK_TABLEBUCKETSAMPLE=960;
	public static final int TOK_TABLECOMMENT=961;
	public static final int TOK_TABLEFILEFORMAT=962;
	public static final int TOK_TABLELOCATION=963;
	public static final int TOK_TABLEPARTCOLS=964;
	public static final int TOK_TABLEPROPERTIES=965;
	public static final int TOK_TABLEPROPERTY=966;
	public static final int TOK_TABLEPROPLIST=967;
	public static final int TOK_TABLEROWFORMAT=968;
	public static final int TOK_TABLEROWFORMATCOLLITEMS=969;
	public static final int TOK_TABLEROWFORMATFIELD=970;
	public static final int TOK_TABLEROWFORMATLINES=971;
	public static final int TOK_TABLEROWFORMATMAPKEYS=972;
	public static final int TOK_TABLEROWFORMATNULL=973;
	public static final int TOK_TABLESERIALIZER=974;
	public static final int TOK_TABLESKEWED=975;
	public static final int TOK_TABLESPLITSAMPLE=976;
	public static final int TOK_TABLE_OR_COL=977;
	public static final int TOK_TABLE_PARTITION=978;
	public static final int TOK_TABLE_TYPE=979;
	public static final int TOK_TABNAME=980;
	public static final int TOK_TABREF=981;
	public static final int TOK_TABSORTCOLNAMEASC=982;
	public static final int TOK_TABSORTCOLNAMEDESC=983;
	public static final int TOK_TABSRC=984;
	public static final int TOK_TABTYPE=985;
	public static final int TOK_TEMPORARY=986;
	public static final int TOK_TIMESTAMP=987;
	public static final int TOK_TIMESTAMPLITERAL=988;
	public static final int TOK_TINYINT=989;
	public static final int TOK_TMP_FILE=990;
	public static final int TOK_TO=991;
	public static final int TOK_TRANSFORM=992;
	public static final int TOK_TRUE=993;
	public static final int TOK_TRUNCATETABLE=994;
	public static final int TOK_TXN_ACCESS_MODE=995;
	public static final int TOK_TXN_READ_ONLY=996;
	public static final int TOK_TXN_READ_WRITE=997;
	public static final int TOK_UNIONALL=998;
	public static final int TOK_UNIONDISTINCT=999;
	public static final int TOK_UNIONTYPE=1000;
	public static final int TOK_UNIQUE=1001;
	public static final int TOK_UNIQUEJOIN=1002;
	public static final int TOK_UNLOCKDB=1003;
	public static final int TOK_UNLOCKTABLE=1004;
	public static final int TOK_UPDATE=1005;
	public static final int TOK_UPDATE_TABLE=1006;
	public static final int TOK_URI_TYPE=1007;
	public static final int TOK_USER=1008;
	public static final int TOK_USERSCRIPTCOLNAMES=1009;
	public static final int TOK_USERSCRIPTCOLSCHEMA=1010;
	public static final int TOK_VALIDATE=1011;
	public static final int TOK_VALUES_TABLE=1012;
	public static final int TOK_VALUE_ROW=1013;
	public static final int TOK_VARCHAR=1014;
	public static final int TOK_VIEWPARTCOLS=1015;
	public static final int TOK_VIRTUAL_TABLE=1016;
	public static final int TOK_VIRTUAL_TABREF=1017;
	public static final int TOK_WHERE=1018;
	public static final int TOK_WINDOWDEF=1019;
	public static final int TOK_WINDOWRANGE=1020;
	public static final int TOK_WINDOWSPEC=1021;
	public static final int TOK_WINDOWVALUES=1022;

	// delegates
	public Parser[] getDelegates() {
		return new Parser[] {};
	}

	// delegators
	public HiveASTParser gHiveASTParser;
	public HiveASTParser gParent;


	public HiveASTParser_FromClauseASTParser(TokenStream input, HiveASTParser gHiveASTParser) {
		this(input, new RecognizerSharedState(), gHiveASTParser);
	}
	public HiveASTParser_FromClauseASTParser(TokenStream input, RecognizerSharedState state, HiveASTParser gHiveASTParser) {
		super(input, state);
		this.gHiveASTParser = gHiveASTParser;
		gParent = gHiveASTParser;
	}

	protected TreeAdaptor adaptor = new CommonTreeAdaptor();

	public void setTreeAdaptor(TreeAdaptor adaptor) {
		this.adaptor = adaptor;
	}
	public TreeAdaptor getTreeAdaptor() {
		return adaptor;
	}
	@Override public String[] getTokenNames() { return HiveASTParser.tokenNames; }
	@Override public String getGrammarFileName() { return "FromClauseASTParser.g"; }


	  @Override
	  public Object recoverFromMismatchedSet(IntStream input,
	      RecognitionException re, BitSet follow) throws RecognitionException {
	    throw re;
	  }
	  @Override
	  public void displayRecognitionError(String[] tokenNames,
	      RecognitionException e) {
	    gParent.errors.add(new HiveASTParseError(gParent, e, tokenNames));
	  }


	public static class tableAllColumns_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "tableAllColumns"
	// FromClauseASTParser.g:50:1: tableAllColumns : ( STAR -> ^( TOK_ALLCOLREF ) | tableName DOT STAR -> ^( TOK_ALLCOLREF tableName ) );
	public final HiveASTParser_FromClauseASTParser.tableAllColumns_return tableAllColumns() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.tableAllColumns_return retval = new HiveASTParser_FromClauseASTParser.tableAllColumns_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token STAR1=null;
		Token DOT3=null;
		Token STAR4=null;
		ParserRuleReturnScope tableName2 =null;

		HiveParserASTNode STAR1_tree=null;
		HiveParserASTNode DOT3_tree=null;
		HiveParserASTNode STAR4_tree=null;
		RewriteRuleTokenStream stream_STAR=new RewriteRuleTokenStream(adaptor,"token STAR");
		RewriteRuleTokenStream stream_DOT=new RewriteRuleTokenStream(adaptor,"token DOT");
		RewriteRuleSubtreeStream stream_tableName=new RewriteRuleSubtreeStream(adaptor,"rule tableName");

		try {
			// FromClauseASTParser.g:51:5: ( STAR -> ^( TOK_ALLCOLREF ) | tableName DOT STAR -> ^( TOK_ALLCOLREF tableName ) )
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==STAR) ) {
				alt1=1;
			}
			else if ( (LA1_0==Identifier||(LA1_0 >= KW_ABORT && LA1_0 <= KW_AFTER)||LA1_0==KW_ANALYZE||LA1_0==KW_ARCHIVE||LA1_0==KW_ASC||(LA1_0 >= KW_AUTOCOMMIT && LA1_0 <= KW_BEFORE)||(LA1_0 >= KW_BUCKET && LA1_0 <= KW_BUCKETS)||(LA1_0 >= KW_CACHE && LA1_0 <= KW_CASCADE)||LA1_0==KW_CHANGE||(LA1_0 >= KW_CLUSTER && LA1_0 <= KW_COLLECTION)||(LA1_0 >= KW_COLUMNS && LA1_0 <= KW_COMMENT)||(LA1_0 >= KW_COMPACT && LA1_0 <= KW_CONCATENATE)||LA1_0==KW_CONTINUE||LA1_0==KW_DATA||LA1_0==KW_DATABASES||(LA1_0 >= KW_DATETIME && LA1_0 <= KW_DBPROPERTIES)||(LA1_0 >= KW_DEFERRED && LA1_0 <= KW_DEFINED)||(LA1_0 >= KW_DELIMITED && LA1_0 <= KW_DESC)||(LA1_0 >= KW_DETAIL && LA1_0 <= KW_DISABLE)||LA1_0==KW_DISTRIBUTE||LA1_0==KW_DOW||(LA1_0 >= KW_DUMP && LA1_0 <= KW_ELEM_TYPE)||LA1_0==KW_ENABLE||LA1_0==KW_ESCAPED||LA1_0==KW_EXCLUSIVE||(LA1_0 >= KW_EXPLAIN && LA1_0 <= KW_EXPRESSION)||(LA1_0 >= KW_FIELDS && LA1_0 <= KW_FIRST)||(LA1_0 >= KW_FORMAT && LA1_0 <= KW_FORMATTED)||LA1_0==KW_FUNCTIONS||(LA1_0 >= KW_HOUR && LA1_0 <= KW_IDXPROPERTIES)||(LA1_0 >= KW_INDEX && LA1_0 <= KW_INDEXES)||(LA1_0 >= KW_INPATH && LA1_0 <= KW_INPUTFORMAT)||(LA1_0 >= KW_ISOLATION && LA1_0 <= KW_JAR)||(LA1_0 >= KW_KEY && LA1_0 <= KW_LAST)||LA1_0==KW_LEVEL||(LA1_0 >= KW_LIMIT && LA1_0 <= KW_LOAD)||(LA1_0 >= KW_LOCATION && LA1_0 <= KW_LONG)||(LA1_0 >= KW_MAPJOIN && LA1_0 <= KW_MATERIALIZED)||LA1_0==KW_METADATA||(LA1_0 >= KW_MINUTE && LA1_0 <= KW_MONTH)||LA1_0==KW_MSCK||(LA1_0 >= KW_NORELY && LA1_0 <= KW_NOSCAN)||LA1_0==KW_NOVALIDATE||LA1_0==KW_NULLS||LA1_0==KW_OFFSET||(LA1_0 >= KW_OPERATOR && LA1_0 <= KW_OPTION)||(LA1_0 >= KW_OUTPUTDRIVER && LA1_0 <= KW_OUTPUTFORMAT)||(LA1_0 >= KW_OVERWRITE && LA1_0 <= KW_OWNER)||(LA1_0 >= KW_PARTITIONED && LA1_0 <= KW_PARTITIONS)||LA1_0==KW_PLUS||LA1_0==KW_PRETTY||LA1_0==KW_PRINCIPALS||(LA1_0 >= KW_PURGE && LA1_0 <= KW_QUARTER)||LA1_0==KW_READ||(LA1_0 >= KW_REBUILD && LA1_0 <= KW_RECORDWRITER)||(LA1_0 >= KW_RELOAD && LA1_0 <= KW_RESTRICT)||LA1_0==KW_REWRITE||(LA1_0 >= KW_ROLE && LA1_0 <= KW_ROLES)||(LA1_0 >= KW_SCHEMA && LA1_0 <= KW_SECOND)||(LA1_0 >= KW_SEMI && LA1_0 <= KW_SERVER)||(LA1_0 >= KW_SETS && LA1_0 <= KW_SKEWED)||(LA1_0 >= KW_SNAPSHOT && LA1_0 <= KW_SSL)||(LA1_0 >= KW_STATISTICS && LA1_0 <= KW_SUMMARY)||LA1_0==KW_TABLES||(LA1_0 >= KW_TBLPROPERTIES && LA1_0 <= KW_TERMINATED)||LA1_0==KW_TINYINT||(LA1_0 >= KW_TOUCH && LA1_0 <= KW_TRANSACTIONS)||LA1_0==KW_UNARCHIVE||LA1_0==KW_UNDO||LA1_0==KW_UNIONTYPE||(LA1_0 >= KW_UNLOCK && LA1_0 <= KW_UNSIGNED)||(LA1_0 >= KW_URI && LA1_0 <= KW_USE)||(LA1_0 >= KW_UTC && LA1_0 <= KW_VALIDATE)||LA1_0==KW_VALUE_TYPE||(LA1_0 >= KW_VECTORIZATION && LA1_0 <= KW_WEEK)||LA1_0==KW_WHILE||(LA1_0 >= KW_WORK && LA1_0 <= KW_YEAR)||LA1_0==KW_BATCH||LA1_0==KW_DAYOFWEEK||LA1_0==KW_HOLD_DDLTIME||LA1_0==KW_IGNORE||LA1_0==KW_NO_DROP||LA1_0==KW_OFFLINE||LA1_0==KW_PROTECTION||LA1_0==KW_READONLY) ) {
				alt1=2;
			}

			else {
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 1, 0, input);
				throw nvae;
			}

			switch (alt1) {
				case 1 :
					// FromClauseASTParser.g:51:7: STAR
					{
					STAR1=(Token)match(input,STAR,FOLLOW_STAR_in_tableAllColumns60); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_STAR.add(STAR1);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 52:9: -> ^( TOK_ALLCOLREF )
					{
						// FromClauseASTParser.g:52:12: ^( TOK_ALLCOLREF )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_ALLCOLREF, "TOK_ALLCOLREF"), root_1);
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;
				case 2 :
					// FromClauseASTParser.g:53:7: tableName DOT STAR
					{
					pushFollow(FOLLOW_tableName_in_tableAllColumns82);
					tableName2=tableName();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_tableName.add(tableName2.getTree());
					DOT3=(Token)match(input,DOT,FOLLOW_DOT_in_tableAllColumns84); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_DOT.add(DOT3);

					STAR4=(Token)match(input,STAR,FOLLOW_STAR_in_tableAllColumns86); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_STAR.add(STAR4);

					// AST REWRITE
					// elements: tableName
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 54:9: -> ^( TOK_ALLCOLREF tableName )
					{
						// FromClauseASTParser.g:54:12: ^( TOK_ALLCOLREF tableName )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_ALLCOLREF, "TOK_ALLCOLREF"), root_1);
						adaptor.addChild(root_1, stream_tableName.nextTree());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "tableAllColumns"


	public static class tableOrColumn_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "tableOrColumn"
	// FromClauseASTParser.g:58:1: tableOrColumn : identifier -> ^( TOK_TABLE_OR_COL identifier ) ;
	public final HiveASTParser_FromClauseASTParser.tableOrColumn_return tableOrColumn() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.tableOrColumn_return retval = new HiveASTParser_FromClauseASTParser.tableOrColumn_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		ParserRuleReturnScope identifier5 =null;

		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");

		 gParent.pushMsg("table or column identifier", state); 
		try {
			// FromClauseASTParser.g:61:5: ( identifier -> ^( TOK_TABLE_OR_COL identifier ) )
			// FromClauseASTParser.g:62:5: identifier
			{
			pushFollow(FOLLOW_identifier_in_tableOrColumn134);
			identifier5=gHiveASTParser.identifier();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_identifier.add(identifier5.getTree());
			// AST REWRITE
			// elements: identifier
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 62:16: -> ^( TOK_TABLE_OR_COL identifier )
			{
				// FromClauseASTParser.g:62:19: ^( TOK_TABLE_OR_COL identifier )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABLE_OR_COL, "TOK_TABLE_OR_COL"), root_1);
				adaptor.addChild(root_1, stream_identifier.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "tableOrColumn"


	public static class expressionList_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "expressionList"
	// FromClauseASTParser.g:65:1: expressionList : expression ( COMMA expression )* -> ^( TOK_EXPLIST ( expression )+ ) ;
	public final HiveASTParser_FromClauseASTParser.expressionList_return expressionList() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.expressionList_return retval = new HiveASTParser_FromClauseASTParser.expressionList_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token COMMA7=null;
		ParserRuleReturnScope expression6 =null;
		ParserRuleReturnScope expression8 =null;

		HiveParserASTNode COMMA7_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");

		 gParent.pushMsg("expression list", state); 
		try {
			// FromClauseASTParser.g:68:5: ( expression ( COMMA expression )* -> ^( TOK_EXPLIST ( expression )+ ) )
			// FromClauseASTParser.g:69:5: expression ( COMMA expression )*
			{
			pushFollow(FOLLOW_expression_in_expressionList173);
			expression6=gHiveASTParser.expression();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_expression.add(expression6.getTree());
			// FromClauseASTParser.g:69:16: ( COMMA expression )*
			loop2:
			while (true) {
				int alt2=2;
				int LA2_0 = input.LA(1);
				if ( (LA2_0==COMMA) ) {
					alt2=1;
				}

				switch (alt2) {
				case 1 :
					// FromClauseASTParser.g:69:17: COMMA expression
					{
					COMMA7=(Token)match(input,COMMA,FOLLOW_COMMA_in_expressionList176); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_COMMA.add(COMMA7);

					pushFollow(FOLLOW_expression_in_expressionList178);
					expression8=gHiveASTParser.expression();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_expression.add(expression8.getTree());
					}
					break;

				default :
					break loop2;
				}
			}

			// AST REWRITE
			// elements: expression
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 69:36: -> ^( TOK_EXPLIST ( expression )+ )
			{
				// FromClauseASTParser.g:69:39: ^( TOK_EXPLIST ( expression )+ )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_EXPLIST, "TOK_EXPLIST"), root_1);
				if ( !(stream_expression.hasNext()) ) {
					throw new RewriteEarlyExitException();
				}
				while ( stream_expression.hasNext() ) {
					adaptor.addChild(root_1, stream_expression.nextTree());
				}
				stream_expression.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "expressionList"


	public static class aliasList_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "aliasList"
	// FromClauseASTParser.g:72:1: aliasList : identifier ( COMMA identifier )* -> ^( TOK_ALIASLIST ( identifier )+ ) ;
	public final HiveASTParser_FromClauseASTParser.aliasList_return aliasList() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.aliasList_return retval = new HiveASTParser_FromClauseASTParser.aliasList_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token COMMA10=null;
		ParserRuleReturnScope identifier9 =null;
		ParserRuleReturnScope identifier11 =null;

		HiveParserASTNode COMMA10_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");

		 gParent.pushMsg("alias list", state); 
		try {
			// FromClauseASTParser.g:75:5: ( identifier ( COMMA identifier )* -> ^( TOK_ALIASLIST ( identifier )+ ) )
			// FromClauseASTParser.g:76:5: identifier ( COMMA identifier )*
			{
			pushFollow(FOLLOW_identifier_in_aliasList220);
			identifier9=gHiveASTParser.identifier();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_identifier.add(identifier9.getTree());
			// FromClauseASTParser.g:76:16: ( COMMA identifier )*
			loop3:
			while (true) {
				int alt3=2;
				int LA3_0 = input.LA(1);
				if ( (LA3_0==COMMA) ) {
					alt3=1;
				}

				switch (alt3) {
				case 1 :
					// FromClauseASTParser.g:76:17: COMMA identifier
					{
					COMMA10=(Token)match(input,COMMA,FOLLOW_COMMA_in_aliasList223); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_COMMA.add(COMMA10);

					pushFollow(FOLLOW_identifier_in_aliasList225);
					identifier11=gHiveASTParser.identifier();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_identifier.add(identifier11.getTree());
					}
					break;

				default :
					break loop3;
				}
			}

			// AST REWRITE
			// elements: identifier
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 76:36: -> ^( TOK_ALIASLIST ( identifier )+ )
			{
				// FromClauseASTParser.g:76:39: ^( TOK_ALIASLIST ( identifier )+ )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_ALIASLIST, "TOK_ALIASLIST"), root_1);
				if ( !(stream_identifier.hasNext()) ) {
					throw new RewriteEarlyExitException();
				}
				while ( stream_identifier.hasNext() ) {
					adaptor.addChild(root_1, stream_identifier.nextTree());
				}
				stream_identifier.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "aliasList"


	public static class fromClause_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "fromClause"
	// FromClauseASTParser.g:81:1: fromClause : KW_FROM fromSource -> ^( TOK_FROM fromSource ) ;
	public final HiveASTParser_FromClauseASTParser.fromClause_return fromClause() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.fromClause_return retval = new HiveASTParser_FromClauseASTParser.fromClause_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_FROM12=null;
		ParserRuleReturnScope fromSource13 =null;

		HiveParserASTNode KW_FROM12_tree=null;
		RewriteRuleTokenStream stream_KW_FROM=new RewriteRuleTokenStream(adaptor,"token KW_FROM");
		RewriteRuleSubtreeStream stream_fromSource=new RewriteRuleSubtreeStream(adaptor,"rule fromSource");

		 gParent.pushMsg("from clause", state); 
		try {
			// FromClauseASTParser.g:84:5: ( KW_FROM fromSource -> ^( TOK_FROM fromSource ) )
			// FromClauseASTParser.g:85:5: KW_FROM fromSource
			{
			KW_FROM12=(Token)match(input,KW_FROM,FOLLOW_KW_FROM_in_fromClause269); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_KW_FROM.add(KW_FROM12);

			pushFollow(FOLLOW_fromSource_in_fromClause271);
			fromSource13=fromSource();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_fromSource.add(fromSource13.getTree());
			// AST REWRITE
			// elements: fromSource
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 85:24: -> ^( TOK_FROM fromSource )
			{
				// FromClauseASTParser.g:85:27: ^( TOK_FROM fromSource )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_FROM, "TOK_FROM"), root_1);
				adaptor.addChild(root_1, stream_fromSource.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "fromClause"


	public static class fromSource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "fromSource"
	// FromClauseASTParser.g:88:1: fromSource : ( virtualTableSource | uniqueJoinToken ^ uniqueJoinSource ( COMMA ! uniqueJoinSource )+ | joinSource );
	public final HiveASTParser_FromClauseASTParser.fromSource_return fromSource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.fromSource_return retval = new HiveASTParser_FromClauseASTParser.fromSource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token COMMA17=null;
		ParserRuleReturnScope virtualTableSource14 =null;
		ParserRuleReturnScope uniqueJoinToken15 =null;
		ParserRuleReturnScope uniqueJoinSource16 =null;
		ParserRuleReturnScope uniqueJoinSource18 =null;
		ParserRuleReturnScope joinSource19 =null;

		HiveParserASTNode COMMA17_tree=null;

		 gParent.pushMsg("join source", state); 
		try {
			// FromClauseASTParser.g:91:5: ( virtualTableSource | uniqueJoinToken ^ uniqueJoinSource ( COMMA ! uniqueJoinSource )+ | joinSource )
			int alt5=3;
			switch ( input.LA(1) ) {
			case LPAREN:
				{
				int LA5_1 = input.LA(2);
				if ( (LA5_1==KW_VALUES) ) {
					alt5=1;
				}
				else if ( (LA5_1==Identifier||(LA5_1 >= KW_ABORT && LA5_1 <= KW_AFTER)||LA5_1==KW_ANALYZE||LA5_1==KW_ARCHIVE||LA5_1==KW_ASC||(LA5_1 >= KW_AUTOCOMMIT && LA5_1 <= KW_BEFORE)||(LA5_1 >= KW_BUCKET && LA5_1 <= KW_BUCKETS)||(LA5_1 >= KW_CACHE && LA5_1 <= KW_CASCADE)||LA5_1==KW_CHANGE||(LA5_1 >= KW_CLUSTER && LA5_1 <= KW_COLLECTION)||(LA5_1 >= KW_COLUMNS && LA5_1 <= KW_COMMENT)||(LA5_1 >= KW_COMPACT && LA5_1 <= KW_CONCATENATE)||LA5_1==KW_CONTINUE||LA5_1==KW_DATA||LA5_1==KW_DATABASES||(LA5_1 >= KW_DATETIME && LA5_1 <= KW_DBPROPERTIES)||(LA5_1 >= KW_DEFERRED && LA5_1 <= KW_DEFINED)||(LA5_1 >= KW_DELIMITED && LA5_1 <= KW_DESC)||(LA5_1 >= KW_DETAIL && LA5_1 <= KW_DISABLE)||LA5_1==KW_DISTRIBUTE||LA5_1==KW_DOW||(LA5_1 >= KW_DUMP && LA5_1 <= KW_ELEM_TYPE)||LA5_1==KW_ENABLE||LA5_1==KW_ESCAPED||LA5_1==KW_EXCLUSIVE||(LA5_1 >= KW_EXPLAIN && LA5_1 <= KW_EXPRESSION)||(LA5_1 >= KW_FIELDS && LA5_1 <= KW_FIRST)||(LA5_1 >= KW_FORMAT && LA5_1 <= KW_FROM)||LA5_1==KW_FUNCTIONS||(LA5_1 >= KW_HOUR && LA5_1 <= KW_IDXPROPERTIES)||(LA5_1 >= KW_INDEX && LA5_1 <= KW_INDEXES)||(LA5_1 >= KW_INPATH && LA5_1 <= KW_INSERT)||(LA5_1 >= KW_ISOLATION && LA5_1 <= KW_JAR)||(LA5_1 >= KW_KEY && LA5_1 <= KW_LAST)||LA5_1==KW_LEVEL||(LA5_1 >= KW_LIMIT && LA5_1 <= KW_LOAD)||(LA5_1 >= KW_LOCATION && LA5_1 <= KW_LONG)||(LA5_1 >= KW_MAP && LA5_1 <= KW_MATERIALIZED)||LA5_1==KW_METADATA||(LA5_1 >= KW_MINUTE && LA5_1 <= KW_MONTH)||LA5_1==KW_MSCK||(LA5_1 >= KW_NORELY && LA5_1 <= KW_NOSCAN)||LA5_1==KW_NOVALIDATE||LA5_1==KW_NULLS||LA5_1==KW_OFFSET||(LA5_1 >= KW_OPERATOR && LA5_1 <= KW_OPTION)||(LA5_1 >= KW_OUTPUTDRIVER && LA5_1 <= KW_OUTPUTFORMAT)||(LA5_1 >= KW_OVERWRITE && LA5_1 <= KW_OWNER)||(LA5_1 >= KW_PARTITIONED && LA5_1 <= KW_PARTITIONS)||LA5_1==KW_PLUS||LA5_1==KW_PRETTY||LA5_1==KW_PRINCIPALS||(LA5_1 >= KW_PURGE && LA5_1 <= KW_QUARTER)||LA5_1==KW_READ||(LA5_1 >= KW_REBUILD && LA5_1 <= KW_REDUCE)||(LA5_1 >= KW_RELOAD && LA5_1 <= KW_RESTRICT)||LA5_1==KW_REWRITE||(LA5_1 >= KW_ROLE && LA5_1 <= KW_ROLES)||(LA5_1 >= KW_SCHEMA && LA5_1 <= KW_SERVER)||(LA5_1 >= KW_SETS && LA5_1 <= KW_SKEWED)||(LA5_1 >= KW_SNAPSHOT && LA5_1 <= KW_SSL)||(LA5_1 >= KW_STATISTICS && LA5_1 <= KW_SUMMARY)||LA5_1==KW_TABLES||(LA5_1 >= KW_TBLPROPERTIES && LA5_1 <= KW_TERMINATED)||LA5_1==KW_TINYINT||(LA5_1 >= KW_TOUCH && LA5_1 <= KW_TRANSACTIONS)||LA5_1==KW_UNARCHIVE||LA5_1==KW_UNDO||LA5_1==KW_UNIONTYPE||(LA5_1 >= KW_UNLOCK && LA5_1 <= KW_UNSIGNED)||(LA5_1 >= KW_URI && LA5_1 <= KW_USE)||(LA5_1 >= KW_UTC && LA5_1 <= KW_VALIDATE)||LA5_1==KW_VALUE_TYPE||(LA5_1 >= KW_VECTORIZATION && LA5_1 <= KW_WEEK)||LA5_1==KW_WHILE||(LA5_1 >= KW_WITH && LA5_1 <= KW_YEAR)||LA5_1==LPAREN||LA5_1==KW_BATCH||LA5_1==KW_DAYOFWEEK||LA5_1==KW_HOLD_DDLTIME||LA5_1==KW_IGNORE||LA5_1==KW_NO_DROP||LA5_1==KW_OFFLINE||LA5_1==KW_PROTECTION||LA5_1==KW_READONLY) ) {
					alt5=3;
				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 5, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case KW_UNIQUEJOIN:
				{
				alt5=2;
				}
				break;
			case Identifier:
			case KW_ABORT:
			case KW_ADD:
			case KW_ADMIN:
			case KW_AFTER:
			case KW_ANALYZE:
			case KW_ARCHIVE:
			case KW_ASC:
			case KW_AUTOCOMMIT:
			case KW_BEFORE:
			case KW_BUCKET:
			case KW_BUCKETS:
			case KW_CACHE:
			case KW_CASCADE:
			case KW_CHANGE:
			case KW_CLUSTER:
			case KW_CLUSTERED:
			case KW_CLUSTERSTATUS:
			case KW_COLLECTION:
			case KW_COLUMNS:
			case KW_COMMENT:
			case KW_COMPACT:
			case KW_COMPACTIONS:
			case KW_COMPUTE:
			case KW_CONCATENATE:
			case KW_CONTINUE:
			case KW_DATA:
			case KW_DATABASES:
			case KW_DATETIME:
			case KW_DAY:
			case KW_DBPROPERTIES:
			case KW_DEFERRED:
			case KW_DEFINED:
			case KW_DELIMITED:
			case KW_DEPENDENCY:
			case KW_DESC:
			case KW_DETAIL:
			case KW_DIRECTORIES:
			case KW_DIRECTORY:
			case KW_DISABLE:
			case KW_DISTRIBUTE:
			case KW_DOW:
			case KW_DUMP:
			case KW_ELEM_TYPE:
			case KW_ENABLE:
			case KW_ESCAPED:
			case KW_EXCLUSIVE:
			case KW_EXPLAIN:
			case KW_EXPORT:
			case KW_EXPRESSION:
			case KW_FIELDS:
			case KW_FILE:
			case KW_FILEFORMAT:
			case KW_FIRST:
			case KW_FORMAT:
			case KW_FORMATTED:
			case KW_FUNCTIONS:
			case KW_HOUR:
			case KW_IDXPROPERTIES:
			case KW_INDEX:
			case KW_INDEXES:
			case KW_INPATH:
			case KW_INPUTDRIVER:
			case KW_INPUTFORMAT:
			case KW_ISOLATION:
			case KW_ITEMS:
			case KW_JAR:
			case KW_KEY:
			case KW_KEYS:
			case KW_KEY_TYPE:
			case KW_LAST:
			case KW_LEVEL:
			case KW_LIMIT:
			case KW_LINES:
			case KW_LOAD:
			case KW_LOCATION:
			case KW_LOCK:
			case KW_LOCKS:
			case KW_LOGICAL:
			case KW_LONG:
			case KW_MAPJOIN:
			case KW_MATCHED:
			case KW_MATERIALIZED:
			case KW_METADATA:
			case KW_MINUTE:
			case KW_MONTH:
			case KW_MSCK:
			case KW_NORELY:
			case KW_NOSCAN:
			case KW_NOVALIDATE:
			case KW_NULLS:
			case KW_OFFSET:
			case KW_OPERATOR:
			case KW_OPTION:
			case KW_OUTPUTDRIVER:
			case KW_OUTPUTFORMAT:
			case KW_OVERWRITE:
			case KW_OWNER:
			case KW_PARTITIONED:
			case KW_PARTITIONS:
			case KW_PLUS:
			case KW_PRETTY:
			case KW_PRINCIPALS:
			case KW_PURGE:
			case KW_QUARTER:
			case KW_READ:
			case KW_REBUILD:
			case KW_RECORDREADER:
			case KW_RECORDWRITER:
			case KW_RELOAD:
			case KW_RELY:
			case KW_RENAME:
			case KW_REPAIR:
			case KW_REPL:
			case KW_REPLACE:
			case KW_REPLICATION:
			case KW_RESTRICT:
			case KW_REWRITE:
			case KW_ROLE:
			case KW_ROLES:
			case KW_SCHEMA:
			case KW_SCHEMAS:
			case KW_SECOND:
			case KW_SEMI:
			case KW_SERDE:
			case KW_SERDEPROPERTIES:
			case KW_SERVER:
			case KW_SETS:
			case KW_SHARED:
			case KW_SHOW:
			case KW_SHOW_DATABASE:
			case KW_SKEWED:
			case KW_SNAPSHOT:
			case KW_SORT:
			case KW_SORTED:
			case KW_SSL:
			case KW_STATISTICS:
			case KW_STATUS:
			case KW_STORED:
			case KW_STREAMTABLE:
			case KW_STRING:
			case KW_STRUCT:
			case KW_SUMMARY:
			case KW_TABLES:
			case KW_TBLPROPERTIES:
			case KW_TEMPORARY:
			case KW_TERMINATED:
			case KW_TINYINT:
			case KW_TOUCH:
			case KW_TRANSACTION:
			case KW_TRANSACTIONS:
			case KW_UNARCHIVE:
			case KW_UNDO:
			case KW_UNIONTYPE:
			case KW_UNLOCK:
			case KW_UNSET:
			case KW_UNSIGNED:
			case KW_URI:
			case KW_USE:
			case KW_UTC:
			case KW_UTCTIMESTAMP:
			case KW_VALIDATE:
			case KW_VALUE_TYPE:
			case KW_VECTORIZATION:
			case KW_VIEW:
			case KW_VIEWS:
			case KW_WAIT:
			case KW_WEEK:
			case KW_WHILE:
			case KW_WORK:
			case KW_WRITE:
			case KW_YEAR:
			case KW_BATCH:
			case KW_DAYOFWEEK:
			case KW_HOLD_DDLTIME:
			case KW_IGNORE:
			case KW_NO_DROP:
			case KW_OFFLINE:
			case KW_PROTECTION:
			case KW_READONLY:
				{
				alt5=3;
				}
				break;
			default:
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 5, 0, input);
				throw nvae;
			}
			switch (alt5) {
				case 1 :
					// FromClauseASTParser.g:92:5: virtualTableSource
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_virtualTableSource_in_fromSource310);
					virtualTableSource14=virtualTableSource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, virtualTableSource14.getTree());

					}
					break;
				case 2 :
					// FromClauseASTParser.g:94:5: uniqueJoinToken ^ uniqueJoinSource ( COMMA ! uniqueJoinSource )+
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_uniqueJoinToken_in_fromSource323);
					uniqueJoinToken15=uniqueJoinToken();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) root_0 = (HiveParserASTNode)adaptor.becomeRoot(uniqueJoinToken15.getTree(), root_0);
					pushFollow(FOLLOW_uniqueJoinSource_in_fromSource326);
					uniqueJoinSource16=uniqueJoinSource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, uniqueJoinSource16.getTree());

					// FromClauseASTParser.g:94:39: ( COMMA ! uniqueJoinSource )+
					int cnt4=0;
					loop4:
					while (true) {
						int alt4=2;
						int LA4_0 = input.LA(1);
						if ( (LA4_0==COMMA) ) {
							alt4=1;
						}

						switch (alt4) {
						case 1 :
							// FromClauseASTParser.g:94:40: COMMA ! uniqueJoinSource
							{
							COMMA17=(Token)match(input,COMMA,FOLLOW_COMMA_in_fromSource329); if (state.failed) return retval;
							pushFollow(FOLLOW_uniqueJoinSource_in_fromSource332);
							uniqueJoinSource18=uniqueJoinSource();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) adaptor.addChild(root_0, uniqueJoinSource18.getTree());

							}
							break;

						default :
							if ( cnt4 >= 1 ) break loop4;
							if (state.backtracking>0) {state.failed=true; return retval;}
							EarlyExitException eee = new EarlyExitException(4, input);
							throw eee;
						}
						cnt4++;
					}

					}
					break;
				case 3 :
					// FromClauseASTParser.g:96:5: joinSource
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_joinSource_in_fromSource346);
					joinSource19=joinSource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, joinSource19.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "fromSource"


	public static class atomjoinSource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "atomjoinSource"
	// FromClauseASTParser.g:100:1: atomjoinSource : ( tableSource ( lateralView ^)* | ( subQuerySource )=> subQuerySource ( lateralView ^)* | partitionedTableFunction ( lateralView ^)* | LPAREN ! joinSource RPAREN !);
	public final HiveASTParser_FromClauseASTParser.atomjoinSource_return atomjoinSource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.atomjoinSource_return retval = new HiveASTParser_FromClauseASTParser.atomjoinSource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token LPAREN26=null;
		Token RPAREN28=null;
		ParserRuleReturnScope tableSource20 =null;
		ParserRuleReturnScope lateralView21 =null;
		ParserRuleReturnScope subQuerySource22 =null;
		ParserRuleReturnScope lateralView23 =null;
		ParserRuleReturnScope partitionedTableFunction24 =null;
		ParserRuleReturnScope lateralView25 =null;
		ParserRuleReturnScope joinSource27 =null;

		HiveParserASTNode LPAREN26_tree=null;
		HiveParserASTNode RPAREN28_tree=null;

		 gParent.pushMsg("joinSource", state); 
		try {
			// FromClauseASTParser.g:103:5: ( tableSource ( lateralView ^)* | ( subQuerySource )=> subQuerySource ( lateralView ^)* | partitionedTableFunction ( lateralView ^)* | LPAREN ! joinSource RPAREN !)
			int alt9=4;
			switch ( input.LA(1) ) {
			case Identifier:
				{
				int LA9_1 = input.LA(2);
				if ( (LA9_1==LPAREN) ) {
					int LA9_4 = input.LA(3);
					if ( (LA9_4==KW_ON) ) {
						alt9=3;
					}
					else if ( (LA9_4==StringLiteral) ) {
						alt9=1;
					}

					else {
						if (state.backtracking>0) {state.failed=true; return retval;}
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 9, 4, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}
				else if ( (LA9_1==EOF||LA9_1==COMMA||LA9_1==DOT||LA9_1==Identifier||(LA9_1 >= KW_ABORT && LA9_1 <= KW_AFTER)||LA9_1==KW_ANALYZE||LA9_1==KW_ARCHIVE||(LA9_1 >= KW_AS && LA9_1 <= KW_ASC)||(LA9_1 >= KW_AUTOCOMMIT && LA9_1 <= KW_BEFORE)||(LA9_1 >= KW_BUCKET && LA9_1 <= KW_BUCKETS)||(LA9_1 >= KW_CACHE && LA9_1 <= KW_CASCADE)||LA9_1==KW_CHANGE||(LA9_1 >= KW_CLUSTER && LA9_1 <= KW_COLLECTION)||(LA9_1 >= KW_COLUMNS && LA9_1 <= KW_COMMENT)||(LA9_1 >= KW_COMPACT && LA9_1 <= KW_CONCATENATE)||LA9_1==KW_CONTINUE||LA9_1==KW_CROSS||LA9_1==KW_DATA||LA9_1==KW_DATABASES||(LA9_1 >= KW_DATETIME && LA9_1 <= KW_DBPROPERTIES)||(LA9_1 >= KW_DEFERRED && LA9_1 <= KW_DEFINED)||(LA9_1 >= KW_DELIMITED && LA9_1 <= KW_DESC)||(LA9_1 >= KW_DETAIL && LA9_1 <= KW_DISABLE)||LA9_1==KW_DISTRIBUTE||LA9_1==KW_DOW||(LA9_1 >= KW_DUMP && LA9_1 <= KW_ELEM_TYPE)||LA9_1==KW_ENABLE||(LA9_1 >= KW_ESCAPED && LA9_1 <= KW_EXCEPT)||LA9_1==KW_EXCLUSIVE||(LA9_1 >= KW_EXPLAIN && LA9_1 <= KW_EXPRESSION)||(LA9_1 >= KW_FIELDS && LA9_1 <= KW_FIRST)||(LA9_1 >= KW_FORMAT && LA9_1 <= KW_FORMATTED)||LA9_1==KW_FULL||LA9_1==KW_FUNCTIONS||LA9_1==KW_GROUP||(LA9_1 >= KW_HAVING && LA9_1 <= KW_IDXPROPERTIES)||(LA9_1 >= KW_INDEX && LA9_1 <= KW_INSERT)||LA9_1==KW_INTERSECT||(LA9_1 >= KW_ISOLATION && LA9_1 <= KW_LEFT)||LA9_1==KW_LEVEL||(LA9_1 >= KW_LIMIT && LA9_1 <= KW_LOAD)||(LA9_1 >= KW_LOCATION && LA9_1 <= KW_LONG)||(LA9_1 >= KW_MAP && LA9_1 <= KW_MATERIALIZED)||(LA9_1 >= KW_METADATA && LA9_1 <= KW_MONTH)||LA9_1==KW_MSCK||(LA9_1 >= KW_NORELY && LA9_1 <= KW_NOSCAN)||LA9_1==KW_NOVALIDATE||LA9_1==KW_NULLS||LA9_1==KW_OFFSET||(LA9_1 >= KW_OPERATOR && LA9_1 <= KW_OPTION)||LA9_1==KW_ORDER||(LA9_1 >= KW_OUTPUTDRIVER && LA9_1 <= KW_OUTPUTFORMAT)||(LA9_1 >= KW_OVERWRITE && LA9_1 <= KW_OWNER)||(LA9_1 >= KW_PARTITIONED && LA9_1 <= KW_PARTITIONS)||LA9_1==KW_PLUS||LA9_1==KW_PRETTY||LA9_1==KW_PRINCIPALS||(LA9_1 >= KW_PURGE && LA9_1 <= KW_QUARTER)||LA9_1==KW_READ||(LA9_1 >= KW_REBUILD && LA9_1 <= KW_REDUCE)||(LA9_1 >= KW_RELOAD && LA9_1 <= KW_RESTRICT)||(LA9_1 >= KW_REWRITE && LA9_1 <= KW_RIGHT)||(LA9_1 >= KW_ROLE && LA9_1 <= KW_ROLES)||(LA9_1 >= KW_SCHEMA && LA9_1 <= KW_SERVER)||(LA9_1 >= KW_SETS && LA9_1 <= KW_SKEWED)||(LA9_1 >= KW_SNAPSHOT && LA9_1 <= KW_SSL)||(LA9_1 >= KW_STATISTICS && LA9_1 <= KW_SUMMARY)||(LA9_1 >= KW_TABLES && LA9_1 <= KW_TERMINATED)||LA9_1==KW_TINYINT||(LA9_1 >= KW_TOUCH && LA9_1 <= KW_TRANSACTIONS)||LA9_1==KW_UNARCHIVE||(LA9_1 >= KW_UNDO && LA9_1 <= KW_UNIONTYPE)||(LA9_1 >= KW_UNLOCK && LA9_1 <= KW_UNSIGNED)||(LA9_1 >= KW_URI && LA9_1 <= KW_USE)||(LA9_1 >= KW_UTC && LA9_1 <= KW_VALIDATE)||LA9_1==KW_VALUE_TYPE||(LA9_1 >= KW_VECTORIZATION && LA9_1 <= KW_WEEK)||(LA9_1 >= KW_WHERE && LA9_1 <= KW_WINDOW)||(LA9_1 >= KW_WORK && LA9_1 <= KW_YEAR)||LA9_1==RPAREN||LA9_1==KW_BATCH||LA9_1==KW_DAYOFWEEK||LA9_1==KW_HOLD_DDLTIME||LA9_1==KW_IGNORE||LA9_1==KW_NO_DROP||LA9_1==KW_OFFLINE||LA9_1==KW_PROTECTION||LA9_1==KW_READONLY) ) {
					alt9=1;
				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 9, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case KW_ABORT:
			case KW_ADD:
			case KW_ADMIN:
			case KW_AFTER:
			case KW_ANALYZE:
			case KW_ARCHIVE:
			case KW_ASC:
			case KW_AUTOCOMMIT:
			case KW_BEFORE:
			case KW_BUCKET:
			case KW_BUCKETS:
			case KW_CACHE:
			case KW_CASCADE:
			case KW_CHANGE:
			case KW_CLUSTER:
			case KW_CLUSTERED:
			case KW_CLUSTERSTATUS:
			case KW_COLLECTION:
			case KW_COLUMNS:
			case KW_COMMENT:
			case KW_COMPACT:
			case KW_COMPACTIONS:
			case KW_COMPUTE:
			case KW_CONCATENATE:
			case KW_CONTINUE:
			case KW_DATA:
			case KW_DATABASES:
			case KW_DATETIME:
			case KW_DAY:
			case KW_DBPROPERTIES:
			case KW_DEFERRED:
			case KW_DEFINED:
			case KW_DELIMITED:
			case KW_DEPENDENCY:
			case KW_DESC:
			case KW_DETAIL:
			case KW_DIRECTORIES:
			case KW_DIRECTORY:
			case KW_DISABLE:
			case KW_DISTRIBUTE:
			case KW_DOW:
			case KW_DUMP:
			case KW_ELEM_TYPE:
			case KW_ENABLE:
			case KW_ESCAPED:
			case KW_EXCLUSIVE:
			case KW_EXPLAIN:
			case KW_EXPORT:
			case KW_EXPRESSION:
			case KW_FIELDS:
			case KW_FILE:
			case KW_FILEFORMAT:
			case KW_FIRST:
			case KW_FORMAT:
			case KW_FORMATTED:
			case KW_FUNCTIONS:
			case KW_HOUR:
			case KW_IDXPROPERTIES:
			case KW_INDEX:
			case KW_INDEXES:
			case KW_INPATH:
			case KW_INPUTDRIVER:
			case KW_INPUTFORMAT:
			case KW_ISOLATION:
			case KW_ITEMS:
			case KW_JAR:
			case KW_KEY:
			case KW_KEYS:
			case KW_KEY_TYPE:
			case KW_LAST:
			case KW_LEVEL:
			case KW_LIMIT:
			case KW_LINES:
			case KW_LOAD:
			case KW_LOCATION:
			case KW_LOCK:
			case KW_LOCKS:
			case KW_LOGICAL:
			case KW_LONG:
			case KW_MAPJOIN:
			case KW_MATCHED:
			case KW_MATERIALIZED:
			case KW_METADATA:
			case KW_MINUTE:
			case KW_MONTH:
			case KW_MSCK:
			case KW_NORELY:
			case KW_NOSCAN:
			case KW_NOVALIDATE:
			case KW_NULLS:
			case KW_OFFSET:
			case KW_OPERATOR:
			case KW_OPTION:
			case KW_OUTPUTDRIVER:
			case KW_OUTPUTFORMAT:
			case KW_OVERWRITE:
			case KW_OWNER:
			case KW_PARTITIONED:
			case KW_PARTITIONS:
			case KW_PLUS:
			case KW_PRETTY:
			case KW_PRINCIPALS:
			case KW_PURGE:
			case KW_QUARTER:
			case KW_READ:
			case KW_REBUILD:
			case KW_RECORDREADER:
			case KW_RECORDWRITER:
			case KW_RELOAD:
			case KW_RELY:
			case KW_RENAME:
			case KW_REPAIR:
			case KW_REPL:
			case KW_REPLACE:
			case KW_REPLICATION:
			case KW_RESTRICT:
			case KW_REWRITE:
			case KW_ROLE:
			case KW_ROLES:
			case KW_SCHEMA:
			case KW_SCHEMAS:
			case KW_SECOND:
			case KW_SEMI:
			case KW_SERDE:
			case KW_SERDEPROPERTIES:
			case KW_SERVER:
			case KW_SETS:
			case KW_SHARED:
			case KW_SHOW:
			case KW_SHOW_DATABASE:
			case KW_SKEWED:
			case KW_SNAPSHOT:
			case KW_SORT:
			case KW_SORTED:
			case KW_SSL:
			case KW_STATISTICS:
			case KW_STATUS:
			case KW_STORED:
			case KW_STREAMTABLE:
			case KW_STRING:
			case KW_STRUCT:
			case KW_SUMMARY:
			case KW_TABLES:
			case KW_TBLPROPERTIES:
			case KW_TEMPORARY:
			case KW_TERMINATED:
			case KW_TINYINT:
			case KW_TOUCH:
			case KW_TRANSACTION:
			case KW_TRANSACTIONS:
			case KW_UNARCHIVE:
			case KW_UNDO:
			case KW_UNIONTYPE:
			case KW_UNLOCK:
			case KW_UNSET:
			case KW_UNSIGNED:
			case KW_URI:
			case KW_USE:
			case KW_UTC:
			case KW_UTCTIMESTAMP:
			case KW_VALIDATE:
			case KW_VALUE_TYPE:
			case KW_VECTORIZATION:
			case KW_VIEW:
			case KW_VIEWS:
			case KW_WAIT:
			case KW_WEEK:
			case KW_WHILE:
			case KW_WORK:
			case KW_WRITE:
			case KW_YEAR:
			case KW_BATCH:
			case KW_DAYOFWEEK:
			case KW_HOLD_DDLTIME:
			case KW_IGNORE:
			case KW_NO_DROP:
			case KW_OFFLINE:
			case KW_PROTECTION:
			case KW_READONLY:
				{
				alt9=1;
				}
				break;
			case LPAREN:
				{
				int LA9_3 = input.LA(2);
				if ( (LA9_3==KW_WITH) && (synpred1_FromClauseASTParser())) {
					alt9=2;
				}
				else if ( (LA9_3==KW_FROM) && (synpred1_FromClauseASTParser())) {
					alt9=2;
				}
				else if ( (LA9_3==KW_INSERT) && (synpred1_FromClauseASTParser())) {
					alt9=2;
				}
				else if ( (LA9_3==KW_SELECT) && (synpred1_FromClauseASTParser())) {
					alt9=2;
				}
				else if ( (LA9_3==KW_MAP) && (synpred1_FromClauseASTParser())) {
					alt9=2;
				}
				else if ( (LA9_3==KW_REDUCE) && (synpred1_FromClauseASTParser())) {
					alt9=2;
				}
				else if ( (LA9_3==LPAREN) ) {
					switch ( input.LA(3) ) {
					case KW_SELECT:
						{
						int LA9_48 = input.LA(4);
						if ( (synpred1_FromClauseASTParser()) ) {
							alt9=2;
						}
						else if ( (true) ) {
							alt9=4;
						}

						}
						break;
					case KW_MAP:
						{
						int LA9_49 = input.LA(4);
						if ( (synpred1_FromClauseASTParser()) ) {
							alt9=2;
						}
						else if ( (true) ) {
							alt9=4;
						}

						}
						break;
					case KW_REDUCE:
						{
						int LA9_50 = input.LA(4);
						if ( (synpred1_FromClauseASTParser()) ) {
							alt9=2;
						}
						else if ( (true) ) {
							alt9=4;
						}

						}
						break;
					case LPAREN:
						{
						int LA9_51 = input.LA(4);
						if ( (synpred1_FromClauseASTParser()) ) {
							alt9=2;
						}
						else if ( (true) ) {
							alt9=4;
						}

						}
						break;
					case Identifier:
					case KW_ABORT:
					case KW_ADD:
					case KW_ADMIN:
					case KW_AFTER:
					case KW_ANALYZE:
					case KW_ARCHIVE:
					case KW_ASC:
					case KW_AUTOCOMMIT:
					case KW_BEFORE:
					case KW_BUCKET:
					case KW_BUCKETS:
					case KW_CACHE:
					case KW_CASCADE:
					case KW_CHANGE:
					case KW_CLUSTER:
					case KW_CLUSTERED:
					case KW_CLUSTERSTATUS:
					case KW_COLLECTION:
					case KW_COLUMNS:
					case KW_COMMENT:
					case KW_COMPACT:
					case KW_COMPACTIONS:
					case KW_COMPUTE:
					case KW_CONCATENATE:
					case KW_CONTINUE:
					case KW_DATA:
					case KW_DATABASES:
					case KW_DATETIME:
					case KW_DAY:
					case KW_DBPROPERTIES:
					case KW_DEFERRED:
					case KW_DEFINED:
					case KW_DELIMITED:
					case KW_DEPENDENCY:
					case KW_DESC:
					case KW_DETAIL:
					case KW_DIRECTORIES:
					case KW_DIRECTORY:
					case KW_DISABLE:
					case KW_DISTRIBUTE:
					case KW_DOW:
					case KW_DUMP:
					case KW_ELEM_TYPE:
					case KW_ENABLE:
					case KW_ESCAPED:
					case KW_EXCLUSIVE:
					case KW_EXPLAIN:
					case KW_EXPORT:
					case KW_EXPRESSION:
					case KW_FIELDS:
					case KW_FILE:
					case KW_FILEFORMAT:
					case KW_FIRST:
					case KW_FORMAT:
					case KW_FORMATTED:
					case KW_FROM:
					case KW_FUNCTIONS:
					case KW_HOUR:
					case KW_IDXPROPERTIES:
					case KW_INDEX:
					case KW_INDEXES:
					case KW_INPATH:
					case KW_INPUTDRIVER:
					case KW_INPUTFORMAT:
					case KW_INSERT:
					case KW_ISOLATION:
					case KW_ITEMS:
					case KW_JAR:
					case KW_KEY:
					case KW_KEYS:
					case KW_KEY_TYPE:
					case KW_LAST:
					case KW_LEVEL:
					case KW_LIMIT:
					case KW_LINES:
					case KW_LOAD:
					case KW_LOCATION:
					case KW_LOCK:
					case KW_LOCKS:
					case KW_LOGICAL:
					case KW_LONG:
					case KW_MAPJOIN:
					case KW_MATCHED:
					case KW_MATERIALIZED:
					case KW_METADATA:
					case KW_MINUTE:
					case KW_MONTH:
					case KW_MSCK:
					case KW_NORELY:
					case KW_NOSCAN:
					case KW_NOVALIDATE:
					case KW_NULLS:
					case KW_OFFSET:
					case KW_OPERATOR:
					case KW_OPTION:
					case KW_OUTPUTDRIVER:
					case KW_OUTPUTFORMAT:
					case KW_OVERWRITE:
					case KW_OWNER:
					case KW_PARTITIONED:
					case KW_PARTITIONS:
					case KW_PLUS:
					case KW_PRETTY:
					case KW_PRINCIPALS:
					case KW_PURGE:
					case KW_QUARTER:
					case KW_READ:
					case KW_REBUILD:
					case KW_RECORDREADER:
					case KW_RECORDWRITER:
					case KW_RELOAD:
					case KW_RELY:
					case KW_RENAME:
					case KW_REPAIR:
					case KW_REPL:
					case KW_REPLACE:
					case KW_REPLICATION:
					case KW_RESTRICT:
					case KW_REWRITE:
					case KW_ROLE:
					case KW_ROLES:
					case KW_SCHEMA:
					case KW_SCHEMAS:
					case KW_SECOND:
					case KW_SEMI:
					case KW_SERDE:
					case KW_SERDEPROPERTIES:
					case KW_SERVER:
					case KW_SETS:
					case KW_SHARED:
					case KW_SHOW:
					case KW_SHOW_DATABASE:
					case KW_SKEWED:
					case KW_SNAPSHOT:
					case KW_SORT:
					case KW_SORTED:
					case KW_SSL:
					case KW_STATISTICS:
					case KW_STATUS:
					case KW_STORED:
					case KW_STREAMTABLE:
					case KW_STRING:
					case KW_STRUCT:
					case KW_SUMMARY:
					case KW_TABLES:
					case KW_TBLPROPERTIES:
					case KW_TEMPORARY:
					case KW_TERMINATED:
					case KW_TINYINT:
					case KW_TOUCH:
					case KW_TRANSACTION:
					case KW_TRANSACTIONS:
					case KW_UNARCHIVE:
					case KW_UNDO:
					case KW_UNIONTYPE:
					case KW_UNLOCK:
					case KW_UNSET:
					case KW_UNSIGNED:
					case KW_URI:
					case KW_USE:
					case KW_UTC:
					case KW_UTCTIMESTAMP:
					case KW_VALIDATE:
					case KW_VALUE_TYPE:
					case KW_VECTORIZATION:
					case KW_VIEW:
					case KW_VIEWS:
					case KW_WAIT:
					case KW_WEEK:
					case KW_WHILE:
					case KW_WITH:
					case KW_WORK:
					case KW_WRITE:
					case KW_YEAR:
					case KW_BATCH:
					case KW_DAYOFWEEK:
					case KW_HOLD_DDLTIME:
					case KW_IGNORE:
					case KW_NO_DROP:
					case KW_OFFLINE:
					case KW_PROTECTION:
					case KW_READONLY:
						{
						alt9=4;
						}
						break;
					default:
						if (state.backtracking>0) {state.failed=true; return retval;}
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 9, 43, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}
				}
				else if ( (LA9_3==Identifier||(LA9_3 >= KW_ABORT && LA9_3 <= KW_AFTER)||LA9_3==KW_ANALYZE||LA9_3==KW_ARCHIVE||LA9_3==KW_ASC||(LA9_3 >= KW_AUTOCOMMIT && LA9_3 <= KW_BEFORE)||(LA9_3 >= KW_BUCKET && LA9_3 <= KW_BUCKETS)||(LA9_3 >= KW_CACHE && LA9_3 <= KW_CASCADE)||LA9_3==KW_CHANGE||(LA9_3 >= KW_CLUSTER && LA9_3 <= KW_COLLECTION)||(LA9_3 >= KW_COLUMNS && LA9_3 <= KW_COMMENT)||(LA9_3 >= KW_COMPACT && LA9_3 <= KW_CONCATENATE)||LA9_3==KW_CONTINUE||LA9_3==KW_DATA||LA9_3==KW_DATABASES||(LA9_3 >= KW_DATETIME && LA9_3 <= KW_DBPROPERTIES)||(LA9_3 >= KW_DEFERRED && LA9_3 <= KW_DEFINED)||(LA9_3 >= KW_DELIMITED && LA9_3 <= KW_DESC)||(LA9_3 >= KW_DETAIL && LA9_3 <= KW_DISABLE)||LA9_3==KW_DISTRIBUTE||LA9_3==KW_DOW||(LA9_3 >= KW_DUMP && LA9_3 <= KW_ELEM_TYPE)||LA9_3==KW_ENABLE||LA9_3==KW_ESCAPED||LA9_3==KW_EXCLUSIVE||(LA9_3 >= KW_EXPLAIN && LA9_3 <= KW_EXPRESSION)||(LA9_3 >= KW_FIELDS && LA9_3 <= KW_FIRST)||(LA9_3 >= KW_FORMAT && LA9_3 <= KW_FORMATTED)||LA9_3==KW_FUNCTIONS||(LA9_3 >= KW_HOUR && LA9_3 <= KW_IDXPROPERTIES)||(LA9_3 >= KW_INDEX && LA9_3 <= KW_INDEXES)||(LA9_3 >= KW_INPATH && LA9_3 <= KW_INPUTFORMAT)||(LA9_3 >= KW_ISOLATION && LA9_3 <= KW_JAR)||(LA9_3 >= KW_KEY && LA9_3 <= KW_LAST)||LA9_3==KW_LEVEL||(LA9_3 >= KW_LIMIT && LA9_3 <= KW_LOAD)||(LA9_3 >= KW_LOCATION && LA9_3 <= KW_LONG)||(LA9_3 >= KW_MAPJOIN && LA9_3 <= KW_MATERIALIZED)||LA9_3==KW_METADATA||(LA9_3 >= KW_MINUTE && LA9_3 <= KW_MONTH)||LA9_3==KW_MSCK||(LA9_3 >= KW_NORELY && LA9_3 <= KW_NOSCAN)||LA9_3==KW_NOVALIDATE||LA9_3==KW_NULLS||LA9_3==KW_OFFSET||(LA9_3 >= KW_OPERATOR && LA9_3 <= KW_OPTION)||(LA9_3 >= KW_OUTPUTDRIVER && LA9_3 <= KW_OUTPUTFORMAT)||(LA9_3 >= KW_OVERWRITE && LA9_3 <= KW_OWNER)||(LA9_3 >= KW_PARTITIONED && LA9_3 <= KW_PARTITIONS)||LA9_3==KW_PLUS||LA9_3==KW_PRETTY||LA9_3==KW_PRINCIPALS||(LA9_3 >= KW_PURGE && LA9_3 <= KW_QUARTER)||LA9_3==KW_READ||(LA9_3 >= KW_REBUILD && LA9_3 <= KW_RECORDWRITER)||(LA9_3 >= KW_RELOAD && LA9_3 <= KW_RESTRICT)||LA9_3==KW_REWRITE||(LA9_3 >= KW_ROLE && LA9_3 <= KW_ROLES)||(LA9_3 >= KW_SCHEMA && LA9_3 <= KW_SECOND)||(LA9_3 >= KW_SEMI && LA9_3 <= KW_SERVER)||(LA9_3 >= KW_SETS && LA9_3 <= KW_SKEWED)||(LA9_3 >= KW_SNAPSHOT && LA9_3 <= KW_SSL)||(LA9_3 >= KW_STATISTICS && LA9_3 <= KW_SUMMARY)||LA9_3==KW_TABLES||(LA9_3 >= KW_TBLPROPERTIES && LA9_3 <= KW_TERMINATED)||LA9_3==KW_TINYINT||(LA9_3 >= KW_TOUCH && LA9_3 <= KW_TRANSACTIONS)||LA9_3==KW_UNARCHIVE||LA9_3==KW_UNDO||LA9_3==KW_UNIONTYPE||(LA9_3 >= KW_UNLOCK && LA9_3 <= KW_UNSIGNED)||(LA9_3 >= KW_URI && LA9_3 <= KW_USE)||(LA9_3 >= KW_UTC && LA9_3 <= KW_VALIDATE)||LA9_3==KW_VALUE_TYPE||(LA9_3 >= KW_VECTORIZATION && LA9_3 <= KW_WEEK)||LA9_3==KW_WHILE||(LA9_3 >= KW_WORK && LA9_3 <= KW_YEAR)||LA9_3==KW_BATCH||LA9_3==KW_DAYOFWEEK||LA9_3==KW_HOLD_DDLTIME||LA9_3==KW_IGNORE||LA9_3==KW_NO_DROP||LA9_3==KW_OFFLINE||LA9_3==KW_PROTECTION||LA9_3==KW_READONLY) ) {
					alt9=4;
				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 9, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			default:
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 9, 0, input);
				throw nvae;
			}
			switch (alt9) {
				case 1 :
					// FromClauseASTParser.g:104:5: tableSource ( lateralView ^)*
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_tableSource_in_atomjoinSource378);
					tableSource20=tableSource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, tableSource20.getTree());

					// FromClauseASTParser.g:104:17: ( lateralView ^)*
					loop6:
					while (true) {
						int alt6=2;
						int LA6_0 = input.LA(1);
						if ( (LA6_0==KW_LATERAL) ) {
							alt6=1;
						}

						switch (alt6) {
						case 1 :
							// FromClauseASTParser.g:104:18: lateralView ^
							{
							pushFollow(FOLLOW_lateralView_in_atomjoinSource381);
							lateralView21=lateralView();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) root_0 = (HiveParserASTNode)adaptor.becomeRoot(lateralView21.getTree(), root_0);
							}
							break;

						default :
							break loop6;
						}
					}

					}
					break;
				case 2 :
					// FromClauseASTParser.g:106:5: ( subQuerySource )=> subQuerySource ( lateralView ^)*
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_subQuerySource_in_atomjoinSource402);
					subQuerySource22=subQuerySource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, subQuerySource22.getTree());

					// FromClauseASTParser.g:106:40: ( lateralView ^)*
					loop7:
					while (true) {
						int alt7=2;
						int LA7_0 = input.LA(1);
						if ( (LA7_0==KW_LATERAL) ) {
							alt7=1;
						}

						switch (alt7) {
						case 1 :
							// FromClauseASTParser.g:106:41: lateralView ^
							{
							pushFollow(FOLLOW_lateralView_in_atomjoinSource405);
							lateralView23=lateralView();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) root_0 = (HiveParserASTNode)adaptor.becomeRoot(lateralView23.getTree(), root_0);
							}
							break;

						default :
							break loop7;
						}
					}

					}
					break;
				case 3 :
					// FromClauseASTParser.g:108:5: partitionedTableFunction ( lateralView ^)*
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_partitionedTableFunction_in_atomjoinSource420);
					partitionedTableFunction24=partitionedTableFunction();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, partitionedTableFunction24.getTree());

					// FromClauseASTParser.g:108:30: ( lateralView ^)*
					loop8:
					while (true) {
						int alt8=2;
						int LA8_0 = input.LA(1);
						if ( (LA8_0==KW_LATERAL) ) {
							alt8=1;
						}

						switch (alt8) {
						case 1 :
							// FromClauseASTParser.g:108:31: lateralView ^
							{
							pushFollow(FOLLOW_lateralView_in_atomjoinSource423);
							lateralView25=lateralView();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) root_0 = (HiveParserASTNode)adaptor.becomeRoot(lateralView25.getTree(), root_0);
							}
							break;

						default :
							break loop8;
						}
					}

					}
					break;
				case 4 :
					// FromClauseASTParser.g:110:5: LPAREN ! joinSource RPAREN !
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					LPAREN26=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_atomjoinSource438); if (state.failed) return retval;
					pushFollow(FOLLOW_joinSource_in_atomjoinSource441);
					joinSource27=joinSource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, joinSource27.getTree());

					RPAREN28=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_atomjoinSource443); if (state.failed) return retval;
					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "atomjoinSource"


	public static class joinSource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "joinSource"
	// FromClauseASTParser.g:113:1: joinSource : atomjoinSource ( joinToken ^ joinSourcePart ( KW_ON ! expression {...}?| KW_USING ! columnParenthesesList {...}?)? )* ;
	public final HiveASTParser_FromClauseASTParser.joinSource_return joinSource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.joinSource_return retval = new HiveASTParser_FromClauseASTParser.joinSource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_ON32=null;
		Token KW_USING34=null;
		ParserRuleReturnScope atomjoinSource29 =null;
		ParserRuleReturnScope joinToken30 =null;
		ParserRuleReturnScope joinSourcePart31 =null;
		ParserRuleReturnScope expression33 =null;
		ParserRuleReturnScope columnParenthesesList35 =null;

		HiveParserASTNode KW_ON32_tree=null;
		HiveParserASTNode KW_USING34_tree=null;

		try {
			// FromClauseASTParser.g:114:5: ( atomjoinSource ( joinToken ^ joinSourcePart ( KW_ON ! expression {...}?| KW_USING ! columnParenthesesList {...}?)? )* )
			// FromClauseASTParser.g:115:5: atomjoinSource ( joinToken ^ joinSourcePart ( KW_ON ! expression {...}?| KW_USING ! columnParenthesesList {...}?)? )*
			{
			root_0 = (HiveParserASTNode)adaptor.nil();


			pushFollow(FOLLOW_atomjoinSource_in_joinSource465);
			atomjoinSource29=atomjoinSource();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) adaptor.addChild(root_0, atomjoinSource29.getTree());

			// FromClauseASTParser.g:115:20: ( joinToken ^ joinSourcePart ( KW_ON ! expression {...}?| KW_USING ! columnParenthesesList {...}?)? )*
			loop11:
			while (true) {
				int alt11=2;
				int LA11_0 = input.LA(1);
				if ( (LA11_0==COMMA||LA11_0==KW_CROSS||LA11_0==KW_FULL||LA11_0==KW_INNER||LA11_0==KW_JOIN||LA11_0==KW_LEFT||LA11_0==KW_RIGHT) ) {
					alt11=1;
				}

				switch (alt11) {
				case 1 :
					// FromClauseASTParser.g:115:21: joinToken ^ joinSourcePart ( KW_ON ! expression {...}?| KW_USING ! columnParenthesesList {...}?)?
					{
					pushFollow(FOLLOW_joinToken_in_joinSource468);
					joinToken30=joinToken();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) root_0 = (HiveParserASTNode)adaptor.becomeRoot(joinToken30.getTree(), root_0);
					pushFollow(FOLLOW_joinSourcePart_in_joinSource471);
					joinSourcePart31=joinSourcePart();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, joinSourcePart31.getTree());

					// FromClauseASTParser.g:115:47: ( KW_ON ! expression {...}?| KW_USING ! columnParenthesesList {...}?)?
					int alt10=3;
					int LA10_0 = input.LA(1);
					if ( (LA10_0==KW_ON) ) {
						alt10=1;
					}
					else if ( (LA10_0==KW_USING) ) {
						alt10=2;
					}
					switch (alt10) {
						case 1 :
							// FromClauseASTParser.g:115:48: KW_ON ! expression {...}?
							{
							KW_ON32=(Token)match(input,KW_ON,FOLLOW_KW_ON_in_joinSource474); if (state.failed) return retval;
							pushFollow(FOLLOW_expression_in_joinSource477);
							expression33=gHiveASTParser.expression();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) adaptor.addChild(root_0, expression33.getTree());

							if ( !(((joinToken30!=null?(joinToken30.start):null).getType() != COMMA)) ) {
								if (state.backtracking>0) {state.failed=true; return retval;}
								throw new FailedPredicateException(input, "joinSource", "$joinToken.start.getType() != COMMA");
							}
							}
							break;
						case 2 :
							// FromClauseASTParser.g:115:107: KW_USING ! columnParenthesesList {...}?
							{
							KW_USING34=(Token)match(input,KW_USING,FOLLOW_KW_USING_in_joinSource483); if (state.failed) return retval;
							pushFollow(FOLLOW_columnParenthesesList_in_joinSource486);
							columnParenthesesList35=gHiveASTParser.columnParenthesesList();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) adaptor.addChild(root_0, columnParenthesesList35.getTree());

							if ( !(((joinToken30!=null?(joinToken30.start):null).getType() != COMMA)) ) {
								if (state.backtracking>0) {state.failed=true; return retval;}
								throw new FailedPredicateException(input, "joinSource", "$joinToken.start.getType() != COMMA");
							}
							}
							break;

					}

					}
					break;

				default :
					break loop11;
				}
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "joinSource"


	public static class joinSourcePart_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "joinSourcePart"
	// FromClauseASTParser.g:118:1: joinSourcePart : ( tableSource | subQuerySource | partitionedTableFunction ) ( lateralView ^)* ;
	public final HiveASTParser_FromClauseASTParser.joinSourcePart_return joinSourcePart() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.joinSourcePart_return retval = new HiveASTParser_FromClauseASTParser.joinSourcePart_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		ParserRuleReturnScope tableSource36 =null;
		ParserRuleReturnScope subQuerySource37 =null;
		ParserRuleReturnScope partitionedTableFunction38 =null;
		ParserRuleReturnScope lateralView39 =null;


		 gParent.pushMsg("joinSourcePart", state); 
		try {
			// FromClauseASTParser.g:121:5: ( ( tableSource | subQuerySource | partitionedTableFunction ) ( lateralView ^)* )
			// FromClauseASTParser.g:122:5: ( tableSource | subQuerySource | partitionedTableFunction ) ( lateralView ^)*
			{
			root_0 = (HiveParserASTNode)adaptor.nil();


			// FromClauseASTParser.g:122:5: ( tableSource | subQuerySource | partitionedTableFunction )
			int alt12=3;
			switch ( input.LA(1) ) {
			case Identifier:
				{
				int LA12_1 = input.LA(2);
				if ( (LA12_1==LPAREN) ) {
					int LA12_4 = input.LA(3);
					if ( (LA12_4==KW_ON) ) {
						alt12=3;
					}
					else if ( (LA12_4==StringLiteral) ) {
						alt12=1;
					}

					else {
						if (state.backtracking>0) {state.failed=true; return retval;}
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 12, 4, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}
				else if ( (LA12_1==EOF||LA12_1==COMMA||LA12_1==DOT||LA12_1==Identifier||(LA12_1 >= KW_ABORT && LA12_1 <= KW_AFTER)||LA12_1==KW_ANALYZE||LA12_1==KW_ARCHIVE||(LA12_1 >= KW_AS && LA12_1 <= KW_ASC)||(LA12_1 >= KW_AUTOCOMMIT && LA12_1 <= KW_BEFORE)||(LA12_1 >= KW_BUCKET && LA12_1 <= KW_BUCKETS)||(LA12_1 >= KW_CACHE && LA12_1 <= KW_CASCADE)||LA12_1==KW_CHANGE||(LA12_1 >= KW_CLUSTER && LA12_1 <= KW_COLLECTION)||(LA12_1 >= KW_COLUMNS && LA12_1 <= KW_COMMENT)||(LA12_1 >= KW_COMPACT && LA12_1 <= KW_CONCATENATE)||LA12_1==KW_CONTINUE||LA12_1==KW_CROSS||LA12_1==KW_DATA||LA12_1==KW_DATABASES||(LA12_1 >= KW_DATETIME && LA12_1 <= KW_DBPROPERTIES)||(LA12_1 >= KW_DEFERRED && LA12_1 <= KW_DEFINED)||(LA12_1 >= KW_DELIMITED && LA12_1 <= KW_DESC)||(LA12_1 >= KW_DETAIL && LA12_1 <= KW_DISABLE)||LA12_1==KW_DISTRIBUTE||LA12_1==KW_DOW||(LA12_1 >= KW_DUMP && LA12_1 <= KW_ELEM_TYPE)||LA12_1==KW_ENABLE||(LA12_1 >= KW_ESCAPED && LA12_1 <= KW_EXCEPT)||LA12_1==KW_EXCLUSIVE||(LA12_1 >= KW_EXPLAIN && LA12_1 <= KW_EXPRESSION)||(LA12_1 >= KW_FIELDS && LA12_1 <= KW_FIRST)||(LA12_1 >= KW_FORMAT && LA12_1 <= KW_FORMATTED)||LA12_1==KW_FULL||LA12_1==KW_FUNCTIONS||LA12_1==KW_GROUP||(LA12_1 >= KW_HAVING && LA12_1 <= KW_IDXPROPERTIES)||(LA12_1 >= KW_INDEX && LA12_1 <= KW_INSERT)||LA12_1==KW_INTERSECT||(LA12_1 >= KW_ISOLATION && LA12_1 <= KW_LEFT)||LA12_1==KW_LEVEL||(LA12_1 >= KW_LIMIT && LA12_1 <= KW_LOAD)||(LA12_1 >= KW_LOCATION && LA12_1 <= KW_LONG)||(LA12_1 >= KW_MAP && LA12_1 <= KW_MATERIALIZED)||(LA12_1 >= KW_METADATA && LA12_1 <= KW_MONTH)||LA12_1==KW_MSCK||(LA12_1 >= KW_NORELY && LA12_1 <= KW_NOSCAN)||LA12_1==KW_NOVALIDATE||LA12_1==KW_NULLS||(LA12_1 >= KW_OFFSET && LA12_1 <= KW_ON)||(LA12_1 >= KW_OPERATOR && LA12_1 <= KW_OPTION)||LA12_1==KW_ORDER||(LA12_1 >= KW_OUTPUTDRIVER && LA12_1 <= KW_OUTPUTFORMAT)||(LA12_1 >= KW_OVERWRITE && LA12_1 <= KW_OWNER)||(LA12_1 >= KW_PARTITIONED && LA12_1 <= KW_PARTITIONS)||LA12_1==KW_PLUS||LA12_1==KW_PRETTY||LA12_1==KW_PRINCIPALS||(LA12_1 >= KW_PURGE && LA12_1 <= KW_QUARTER)||LA12_1==KW_READ||(LA12_1 >= KW_REBUILD && LA12_1 <= KW_REDUCE)||(LA12_1 >= KW_RELOAD && LA12_1 <= KW_RESTRICT)||(LA12_1 >= KW_REWRITE && LA12_1 <= KW_RIGHT)||(LA12_1 >= KW_ROLE && LA12_1 <= KW_ROLES)||(LA12_1 >= KW_SCHEMA && LA12_1 <= KW_SERVER)||(LA12_1 >= KW_SETS && LA12_1 <= KW_SKEWED)||(LA12_1 >= KW_SNAPSHOT && LA12_1 <= KW_SSL)||(LA12_1 >= KW_STATISTICS && LA12_1 <= KW_SUMMARY)||(LA12_1 >= KW_TABLES && LA12_1 <= KW_TERMINATED)||LA12_1==KW_TINYINT||(LA12_1 >= KW_TOUCH && LA12_1 <= KW_TRANSACTIONS)||LA12_1==KW_UNARCHIVE||(LA12_1 >= KW_UNDO && LA12_1 <= KW_UNIONTYPE)||(LA12_1 >= KW_UNLOCK && LA12_1 <= KW_UNSIGNED)||(LA12_1 >= KW_URI && LA12_1 <= KW_USE)||(LA12_1 >= KW_USING && LA12_1 <= KW_VALIDATE)||LA12_1==KW_VALUE_TYPE||(LA12_1 >= KW_VECTORIZATION && LA12_1 <= KW_WEEK)||(LA12_1 >= KW_WHERE && LA12_1 <= KW_WINDOW)||(LA12_1 >= KW_WORK && LA12_1 <= KW_YEAR)||LA12_1==RPAREN||LA12_1==KW_BATCH||LA12_1==KW_DAYOFWEEK||LA12_1==KW_HOLD_DDLTIME||LA12_1==KW_IGNORE||LA12_1==KW_NO_DROP||LA12_1==KW_OFFLINE||LA12_1==KW_PROTECTION||LA12_1==KW_READONLY) ) {
					alt12=1;
				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 12, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case KW_ABORT:
			case KW_ADD:
			case KW_ADMIN:
			case KW_AFTER:
			case KW_ANALYZE:
			case KW_ARCHIVE:
			case KW_ASC:
			case KW_AUTOCOMMIT:
			case KW_BEFORE:
			case KW_BUCKET:
			case KW_BUCKETS:
			case KW_CACHE:
			case KW_CASCADE:
			case KW_CHANGE:
			case KW_CLUSTER:
			case KW_CLUSTERED:
			case KW_CLUSTERSTATUS:
			case KW_COLLECTION:
			case KW_COLUMNS:
			case KW_COMMENT:
			case KW_COMPACT:
			case KW_COMPACTIONS:
			case KW_COMPUTE:
			case KW_CONCATENATE:
			case KW_CONTINUE:
			case KW_DATA:
			case KW_DATABASES:
			case KW_DATETIME:
			case KW_DAY:
			case KW_DBPROPERTIES:
			case KW_DEFERRED:
			case KW_DEFINED:
			case KW_DELIMITED:
			case KW_DEPENDENCY:
			case KW_DESC:
			case KW_DETAIL:
			case KW_DIRECTORIES:
			case KW_DIRECTORY:
			case KW_DISABLE:
			case KW_DISTRIBUTE:
			case KW_DOW:
			case KW_DUMP:
			case KW_ELEM_TYPE:
			case KW_ENABLE:
			case KW_ESCAPED:
			case KW_EXCLUSIVE:
			case KW_EXPLAIN:
			case KW_EXPORT:
			case KW_EXPRESSION:
			case KW_FIELDS:
			case KW_FILE:
			case KW_FILEFORMAT:
			case KW_FIRST:
			case KW_FORMAT:
			case KW_FORMATTED:
			case KW_FUNCTIONS:
			case KW_HOUR:
			case KW_IDXPROPERTIES:
			case KW_INDEX:
			case KW_INDEXES:
			case KW_INPATH:
			case KW_INPUTDRIVER:
			case KW_INPUTFORMAT:
			case KW_ISOLATION:
			case KW_ITEMS:
			case KW_JAR:
			case KW_KEY:
			case KW_KEYS:
			case KW_KEY_TYPE:
			case KW_LAST:
			case KW_LEVEL:
			case KW_LIMIT:
			case KW_LINES:
			case KW_LOAD:
			case KW_LOCATION:
			case KW_LOCK:
			case KW_LOCKS:
			case KW_LOGICAL:
			case KW_LONG:
			case KW_MAPJOIN:
			case KW_MATCHED:
			case KW_MATERIALIZED:
			case KW_METADATA:
			case KW_MINUTE:
			case KW_MONTH:
			case KW_MSCK:
			case KW_NORELY:
			case KW_NOSCAN:
			case KW_NOVALIDATE:
			case KW_NULLS:
			case KW_OFFSET:
			case KW_OPERATOR:
			case KW_OPTION:
			case KW_OUTPUTDRIVER:
			case KW_OUTPUTFORMAT:
			case KW_OVERWRITE:
			case KW_OWNER:
			case KW_PARTITIONED:
			case KW_PARTITIONS:
			case KW_PLUS:
			case KW_PRETTY:
			case KW_PRINCIPALS:
			case KW_PURGE:
			case KW_QUARTER:
			case KW_READ:
			case KW_REBUILD:
			case KW_RECORDREADER:
			case KW_RECORDWRITER:
			case KW_RELOAD:
			case KW_RELY:
			case KW_RENAME:
			case KW_REPAIR:
			case KW_REPL:
			case KW_REPLACE:
			case KW_REPLICATION:
			case KW_RESTRICT:
			case KW_REWRITE:
			case KW_ROLE:
			case KW_ROLES:
			case KW_SCHEMA:
			case KW_SCHEMAS:
			case KW_SECOND:
			case KW_SEMI:
			case KW_SERDE:
			case KW_SERDEPROPERTIES:
			case KW_SERVER:
			case KW_SETS:
			case KW_SHARED:
			case KW_SHOW:
			case KW_SHOW_DATABASE:
			case KW_SKEWED:
			case KW_SNAPSHOT:
			case KW_SORT:
			case KW_SORTED:
			case KW_SSL:
			case KW_STATISTICS:
			case KW_STATUS:
			case KW_STORED:
			case KW_STREAMTABLE:
			case KW_STRING:
			case KW_STRUCT:
			case KW_SUMMARY:
			case KW_TABLES:
			case KW_TBLPROPERTIES:
			case KW_TEMPORARY:
			case KW_TERMINATED:
			case KW_TINYINT:
			case KW_TOUCH:
			case KW_TRANSACTION:
			case KW_TRANSACTIONS:
			case KW_UNARCHIVE:
			case KW_UNDO:
			case KW_UNIONTYPE:
			case KW_UNLOCK:
			case KW_UNSET:
			case KW_UNSIGNED:
			case KW_URI:
			case KW_USE:
			case KW_UTC:
			case KW_UTCTIMESTAMP:
			case KW_VALIDATE:
			case KW_VALUE_TYPE:
			case KW_VECTORIZATION:
			case KW_VIEW:
			case KW_VIEWS:
			case KW_WAIT:
			case KW_WEEK:
			case KW_WHILE:
			case KW_WORK:
			case KW_WRITE:
			case KW_YEAR:
			case KW_BATCH:
			case KW_DAYOFWEEK:
			case KW_HOLD_DDLTIME:
			case KW_IGNORE:
			case KW_NO_DROP:
			case KW_OFFLINE:
			case KW_PROTECTION:
			case KW_READONLY:
				{
				alt12=1;
				}
				break;
			case LPAREN:
				{
				alt12=2;
				}
				break;
			default:
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 12, 0, input);
				throw nvae;
			}
			switch (alt12) {
				case 1 :
					// FromClauseASTParser.g:122:6: tableSource
					{
					pushFollow(FOLLOW_tableSource_in_joinSourcePart524);
					tableSource36=tableSource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, tableSource36.getTree());

					}
					break;
				case 2 :
					// FromClauseASTParser.g:122:20: subQuerySource
					{
					pushFollow(FOLLOW_subQuerySource_in_joinSourcePart528);
					subQuerySource37=subQuerySource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, subQuerySource37.getTree());

					}
					break;
				case 3 :
					// FromClauseASTParser.g:122:37: partitionedTableFunction
					{
					pushFollow(FOLLOW_partitionedTableFunction_in_joinSourcePart532);
					partitionedTableFunction38=partitionedTableFunction();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, partitionedTableFunction38.getTree());

					}
					break;

			}

			// FromClauseASTParser.g:122:63: ( lateralView ^)*
			loop13:
			while (true) {
				int alt13=2;
				int LA13_0 = input.LA(1);
				if ( (LA13_0==KW_LATERAL) ) {
					alt13=1;
				}

				switch (alt13) {
				case 1 :
					// FromClauseASTParser.g:122:64: lateralView ^
					{
					pushFollow(FOLLOW_lateralView_in_joinSourcePart536);
					lateralView39=lateralView();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) root_0 = (HiveParserASTNode)adaptor.becomeRoot(lateralView39.getTree(), root_0);
					}
					break;

				default :
					break loop13;
				}
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "joinSourcePart"


	public static class uniqueJoinSource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "uniqueJoinSource"
	// FromClauseASTParser.g:125:1: uniqueJoinSource : ( KW_PRESERVE )? uniqueJoinTableSource uniqueJoinExpr ;
	public final HiveASTParser_FromClauseASTParser.uniqueJoinSource_return uniqueJoinSource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.uniqueJoinSource_return retval = new HiveASTParser_FromClauseASTParser.uniqueJoinSource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_PRESERVE40=null;
		ParserRuleReturnScope uniqueJoinTableSource41 =null;
		ParserRuleReturnScope uniqueJoinExpr42 =null;

		HiveParserASTNode KW_PRESERVE40_tree=null;

		 gParent.pushMsg("unique join source", state); 
		try {
			// FromClauseASTParser.g:128:5: ( ( KW_PRESERVE )? uniqueJoinTableSource uniqueJoinExpr )
			// FromClauseASTParser.g:128:7: ( KW_PRESERVE )? uniqueJoinTableSource uniqueJoinExpr
			{
			root_0 = (HiveParserASTNode)adaptor.nil();


			// FromClauseASTParser.g:128:7: ( KW_PRESERVE )?
			int alt14=2;
			int LA14_0 = input.LA(1);
			if ( (LA14_0==KW_PRESERVE) ) {
				alt14=1;
			}
			switch (alt14) {
				case 1 :
					// FromClauseASTParser.g:128:7: KW_PRESERVE
					{
					KW_PRESERVE40=(Token)match(input,KW_PRESERVE,FOLLOW_KW_PRESERVE_in_uniqueJoinSource566); if (state.failed) return retval;
					if ( state.backtracking==0 ) {
					KW_PRESERVE40_tree = (HiveParserASTNode)adaptor.create(KW_PRESERVE40);
					adaptor.addChild(root_0, KW_PRESERVE40_tree);
					}

					}
					break;

			}

			pushFollow(FOLLOW_uniqueJoinTableSource_in_uniqueJoinSource569);
			uniqueJoinTableSource41=uniqueJoinTableSource();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) adaptor.addChild(root_0, uniqueJoinTableSource41.getTree());

			pushFollow(FOLLOW_uniqueJoinExpr_in_uniqueJoinSource571);
			uniqueJoinExpr42=uniqueJoinExpr();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) adaptor.addChild(root_0, uniqueJoinExpr42.getTree());

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "uniqueJoinSource"


	public static class uniqueJoinExpr_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "uniqueJoinExpr"
	// FromClauseASTParser.g:131:1: uniqueJoinExpr : LPAREN ! expressionList RPAREN !;
	public final HiveASTParser_FromClauseASTParser.uniqueJoinExpr_return uniqueJoinExpr() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.uniqueJoinExpr_return retval = new HiveASTParser_FromClauseASTParser.uniqueJoinExpr_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token LPAREN43=null;
		Token RPAREN45=null;
		ParserRuleReturnScope expressionList44 =null;

		HiveParserASTNode LPAREN43_tree=null;
		HiveParserASTNode RPAREN45_tree=null;

		 gParent.pushMsg("unique join expression list", state); 
		try {
			// FromClauseASTParser.g:134:5: ( LPAREN ! expressionList RPAREN !)
			// FromClauseASTParser.g:134:7: LPAREN ! expressionList RPAREN !
			{
			root_0 = (HiveParserASTNode)adaptor.nil();


			LPAREN43=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_uniqueJoinExpr598); if (state.failed) return retval;
			pushFollow(FOLLOW_expressionList_in_uniqueJoinExpr601);
			expressionList44=expressionList();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) adaptor.addChild(root_0, expressionList44.getTree());

			RPAREN45=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_uniqueJoinExpr603); if (state.failed) return retval;
			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "uniqueJoinExpr"


	public static class uniqueJoinToken_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "uniqueJoinToken"
	// FromClauseASTParser.g:137:1: uniqueJoinToken : KW_UNIQUEJOIN -> TOK_UNIQUEJOIN ;
	public final HiveASTParser_FromClauseASTParser.uniqueJoinToken_return uniqueJoinToken() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.uniqueJoinToken_return retval = new HiveASTParser_FromClauseASTParser.uniqueJoinToken_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_UNIQUEJOIN46=null;

		HiveParserASTNode KW_UNIQUEJOIN46_tree=null;
		RewriteRuleTokenStream stream_KW_UNIQUEJOIN=new RewriteRuleTokenStream(adaptor,"token KW_UNIQUEJOIN");

		 gParent.pushMsg("unique join", state); 
		try {
			// FromClauseASTParser.g:140:5: ( KW_UNIQUEJOIN -> TOK_UNIQUEJOIN )
			// FromClauseASTParser.g:140:7: KW_UNIQUEJOIN
			{
			KW_UNIQUEJOIN46=(Token)match(input,KW_UNIQUEJOIN,FOLLOW_KW_UNIQUEJOIN_in_uniqueJoinToken631); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_KW_UNIQUEJOIN.add(KW_UNIQUEJOIN46);

			// AST REWRITE
			// elements: 
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 140:21: -> TOK_UNIQUEJOIN
			{
				adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_UNIQUEJOIN, "TOK_UNIQUEJOIN"));
			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "uniqueJoinToken"


	public static class joinToken_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "joinToken"
	// FromClauseASTParser.g:142:1: joinToken : ( KW_JOIN -> TOK_JOIN | KW_INNER KW_JOIN -> TOK_JOIN | COMMA -> TOK_JOIN | KW_CROSS KW_JOIN -> TOK_CROSSJOIN | KW_LEFT ( KW_OUTER )? KW_JOIN -> TOK_LEFTOUTERJOIN | KW_RIGHT ( KW_OUTER )? KW_JOIN -> TOK_RIGHTOUTERJOIN | KW_FULL ( KW_OUTER )? KW_JOIN -> TOK_FULLOUTERJOIN | KW_LEFT KW_SEMI KW_JOIN -> TOK_LEFTSEMIJOIN );
	public final HiveASTParser_FromClauseASTParser.joinToken_return joinToken() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.joinToken_return retval = new HiveASTParser_FromClauseASTParser.joinToken_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_JOIN47=null;
		Token KW_INNER48=null;
		Token KW_JOIN49=null;
		Token COMMA50=null;
		Token KW_CROSS51=null;
		Token KW_JOIN52=null;
		Token KW_LEFT53=null;
		Token KW_OUTER54=null;
		Token KW_JOIN55=null;
		Token KW_RIGHT56=null;
		Token KW_OUTER57=null;
		Token KW_JOIN58=null;
		Token KW_FULL59=null;
		Token KW_OUTER60=null;
		Token KW_JOIN61=null;
		Token KW_LEFT62=null;
		Token KW_SEMI63=null;
		Token KW_JOIN64=null;

		HiveParserASTNode KW_JOIN47_tree=null;
		HiveParserASTNode KW_INNER48_tree=null;
		HiveParserASTNode KW_JOIN49_tree=null;
		HiveParserASTNode COMMA50_tree=null;
		HiveParserASTNode KW_CROSS51_tree=null;
		HiveParserASTNode KW_JOIN52_tree=null;
		HiveParserASTNode KW_LEFT53_tree=null;
		HiveParserASTNode KW_OUTER54_tree=null;
		HiveParserASTNode KW_JOIN55_tree=null;
		HiveParserASTNode KW_RIGHT56_tree=null;
		HiveParserASTNode KW_OUTER57_tree=null;
		HiveParserASTNode KW_JOIN58_tree=null;
		HiveParserASTNode KW_FULL59_tree=null;
		HiveParserASTNode KW_OUTER60_tree=null;
		HiveParserASTNode KW_JOIN61_tree=null;
		HiveParserASTNode KW_LEFT62_tree=null;
		HiveParserASTNode KW_SEMI63_tree=null;
		HiveParserASTNode KW_JOIN64_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_KW_RIGHT=new RewriteRuleTokenStream(adaptor,"token KW_RIGHT");
		RewriteRuleTokenStream stream_KW_CROSS=new RewriteRuleTokenStream(adaptor,"token KW_CROSS");
		RewriteRuleTokenStream stream_KW_FULL=new RewriteRuleTokenStream(adaptor,"token KW_FULL");
		RewriteRuleTokenStream stream_KW_JOIN=new RewriteRuleTokenStream(adaptor,"token KW_JOIN");
		RewriteRuleTokenStream stream_KW_OUTER=new RewriteRuleTokenStream(adaptor,"token KW_OUTER");
		RewriteRuleTokenStream stream_KW_SEMI=new RewriteRuleTokenStream(adaptor,"token KW_SEMI");
		RewriteRuleTokenStream stream_KW_LEFT=new RewriteRuleTokenStream(adaptor,"token KW_LEFT");
		RewriteRuleTokenStream stream_KW_INNER=new RewriteRuleTokenStream(adaptor,"token KW_INNER");

		 gParent.pushMsg("join type specifier", state); 
		try {
			// FromClauseASTParser.g:145:5: ( KW_JOIN -> TOK_JOIN | KW_INNER KW_JOIN -> TOK_JOIN | COMMA -> TOK_JOIN | KW_CROSS KW_JOIN -> TOK_CROSSJOIN | KW_LEFT ( KW_OUTER )? KW_JOIN -> TOK_LEFTOUTERJOIN | KW_RIGHT ( KW_OUTER )? KW_JOIN -> TOK_RIGHTOUTERJOIN | KW_FULL ( KW_OUTER )? KW_JOIN -> TOK_FULLOUTERJOIN | KW_LEFT KW_SEMI KW_JOIN -> TOK_LEFTSEMIJOIN )
			int alt18=8;
			switch ( input.LA(1) ) {
			case KW_JOIN:
				{
				alt18=1;
				}
				break;
			case KW_INNER:
				{
				alt18=2;
				}
				break;
			case COMMA:
				{
				alt18=3;
				}
				break;
			case KW_CROSS:
				{
				alt18=4;
				}
				break;
			case KW_LEFT:
				{
				int LA18_5 = input.LA(2);
				if ( (LA18_5==KW_SEMI) ) {
					alt18=8;
				}
				else if ( (LA18_5==KW_JOIN||LA18_5==KW_OUTER) ) {
					alt18=5;
				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 18, 5, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case KW_RIGHT:
				{
				alt18=6;
				}
				break;
			case KW_FULL:
				{
				alt18=7;
				}
				break;
			default:
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 18, 0, input);
				throw nvae;
			}
			switch (alt18) {
				case 1 :
					// FromClauseASTParser.g:146:7: KW_JOIN
					{
					KW_JOIN47=(Token)match(input,KW_JOIN,FOLLOW_KW_JOIN_in_joinToken663); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_JOIN.add(KW_JOIN47);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 146:36: -> TOK_JOIN
					{
						adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_JOIN, "TOK_JOIN"));
					}


					retval.tree = root_0;
					}

					}
					break;
				case 2 :
					// FromClauseASTParser.g:147:7: KW_INNER KW_JOIN
					{
					KW_INNER48=(Token)match(input,KW_INNER,FOLLOW_KW_INNER_in_joinToken696); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_INNER.add(KW_INNER48);

					KW_JOIN49=(Token)match(input,KW_JOIN,FOLLOW_KW_JOIN_in_joinToken698); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_JOIN.add(KW_JOIN49);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 147:36: -> TOK_JOIN
					{
						adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_JOIN, "TOK_JOIN"));
					}


					retval.tree = root_0;
					}

					}
					break;
				case 3 :
					// FromClauseASTParser.g:148:7: COMMA
					{
					COMMA50=(Token)match(input,COMMA,FOLLOW_COMMA_in_joinToken722); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_COMMA.add(COMMA50);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 148:36: -> TOK_JOIN
					{
						adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_JOIN, "TOK_JOIN"));
					}


					retval.tree = root_0;
					}

					}
					break;
				case 4 :
					// FromClauseASTParser.g:149:7: KW_CROSS KW_JOIN
					{
					KW_CROSS51=(Token)match(input,KW_CROSS,FOLLOW_KW_CROSS_in_joinToken757); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_CROSS.add(KW_CROSS51);

					KW_JOIN52=(Token)match(input,KW_JOIN,FOLLOW_KW_JOIN_in_joinToken759); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_JOIN.add(KW_JOIN52);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 149:36: -> TOK_CROSSJOIN
					{
						adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_CROSSJOIN, "TOK_CROSSJOIN"));
					}


					retval.tree = root_0;
					}

					}
					break;
				case 5 :
					// FromClauseASTParser.g:150:7: KW_LEFT ( KW_OUTER )? KW_JOIN
					{
					KW_LEFT53=(Token)match(input,KW_LEFT,FOLLOW_KW_LEFT_in_joinToken783); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_LEFT.add(KW_LEFT53);

					// FromClauseASTParser.g:150:16: ( KW_OUTER )?
					int alt15=2;
					int LA15_0 = input.LA(1);
					if ( (LA15_0==KW_OUTER) ) {
						alt15=1;
					}
					switch (alt15) {
						case 1 :
							// FromClauseASTParser.g:150:17: KW_OUTER
							{
							KW_OUTER54=(Token)match(input,KW_OUTER,FOLLOW_KW_OUTER_in_joinToken787); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_OUTER.add(KW_OUTER54);

							}
							break;

					}

					KW_JOIN55=(Token)match(input,KW_JOIN,FOLLOW_KW_JOIN_in_joinToken791); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_JOIN.add(KW_JOIN55);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 150:36: -> TOK_LEFTOUTERJOIN
					{
						adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_LEFTOUTERJOIN, "TOK_LEFTOUTERJOIN"));
					}


					retval.tree = root_0;
					}

					}
					break;
				case 6 :
					// FromClauseASTParser.g:151:7: KW_RIGHT ( KW_OUTER )? KW_JOIN
					{
					KW_RIGHT56=(Token)match(input,KW_RIGHT,FOLLOW_KW_RIGHT_in_joinToken803); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_RIGHT.add(KW_RIGHT56);

					// FromClauseASTParser.g:151:16: ( KW_OUTER )?
					int alt16=2;
					int LA16_0 = input.LA(1);
					if ( (LA16_0==KW_OUTER) ) {
						alt16=1;
					}
					switch (alt16) {
						case 1 :
							// FromClauseASTParser.g:151:17: KW_OUTER
							{
							KW_OUTER57=(Token)match(input,KW_OUTER,FOLLOW_KW_OUTER_in_joinToken806); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_OUTER.add(KW_OUTER57);

							}
							break;

					}

					KW_JOIN58=(Token)match(input,KW_JOIN,FOLLOW_KW_JOIN_in_joinToken810); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_JOIN.add(KW_JOIN58);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 151:36: -> TOK_RIGHTOUTERJOIN
					{
						adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_RIGHTOUTERJOIN, "TOK_RIGHTOUTERJOIN"));
					}


					retval.tree = root_0;
					}

					}
					break;
				case 7 :
					// FromClauseASTParser.g:152:7: KW_FULL ( KW_OUTER )? KW_JOIN
					{
					KW_FULL59=(Token)match(input,KW_FULL,FOLLOW_KW_FULL_in_joinToken822); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_FULL.add(KW_FULL59);

					// FromClauseASTParser.g:152:16: ( KW_OUTER )?
					int alt17=2;
					int LA17_0 = input.LA(1);
					if ( (LA17_0==KW_OUTER) ) {
						alt17=1;
					}
					switch (alt17) {
						case 1 :
							// FromClauseASTParser.g:152:17: KW_OUTER
							{
							KW_OUTER60=(Token)match(input,KW_OUTER,FOLLOW_KW_OUTER_in_joinToken826); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_OUTER.add(KW_OUTER60);

							}
							break;

					}

					KW_JOIN61=(Token)match(input,KW_JOIN,FOLLOW_KW_JOIN_in_joinToken830); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_JOIN.add(KW_JOIN61);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 152:36: -> TOK_FULLOUTERJOIN
					{
						adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_FULLOUTERJOIN, "TOK_FULLOUTERJOIN"));
					}


					retval.tree = root_0;
					}

					}
					break;
				case 8 :
					// FromClauseASTParser.g:153:7: KW_LEFT KW_SEMI KW_JOIN
					{
					KW_LEFT62=(Token)match(input,KW_LEFT,FOLLOW_KW_LEFT_in_joinToken842); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_LEFT.add(KW_LEFT62);

					KW_SEMI63=(Token)match(input,KW_SEMI,FOLLOW_KW_SEMI_in_joinToken844); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_SEMI.add(KW_SEMI63);

					KW_JOIN64=(Token)match(input,KW_JOIN,FOLLOW_KW_JOIN_in_joinToken846); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_JOIN.add(KW_JOIN64);

					// AST REWRITE
					// elements: 
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 153:36: -> TOK_LEFTSEMIJOIN
					{
						adaptor.addChild(root_0, (HiveParserASTNode)adaptor.create(TOK_LEFTSEMIJOIN, "TOK_LEFTSEMIJOIN"));
					}


					retval.tree = root_0;
					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "joinToken"


	public static class lateralView_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "lateralView"
	// FromClauseASTParser.g:156:1: lateralView : ( ( KW_LATERAL KW_VIEW KW_OUTER )=> KW_LATERAL KW_VIEW KW_OUTER function tableAlias ( KW_AS identifier ( ( COMMA )=> COMMA identifier )* )? -> ^( TOK_LATERAL_VIEW_OUTER ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) ) ) | KW_LATERAL KW_VIEW function tableAlias ( KW_AS identifier ( ( COMMA )=> COMMA identifier )* )? -> ^( TOK_LATERAL_VIEW ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) ) ) );
	public final HiveASTParser_FromClauseASTParser.lateralView_return lateralView() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.lateralView_return retval = new HiveASTParser_FromClauseASTParser.lateralView_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_LATERAL65=null;
		Token KW_VIEW66=null;
		Token KW_OUTER67=null;
		Token KW_AS70=null;
		Token COMMA72=null;
		Token KW_LATERAL74=null;
		Token KW_VIEW75=null;
		Token KW_AS78=null;
		Token COMMA80=null;
		ParserRuleReturnScope function68 =null;
		ParserRuleReturnScope tableAlias69 =null;
		ParserRuleReturnScope identifier71 =null;
		ParserRuleReturnScope identifier73 =null;
		ParserRuleReturnScope function76 =null;
		ParserRuleReturnScope tableAlias77 =null;
		ParserRuleReturnScope identifier79 =null;
		ParserRuleReturnScope identifier81 =null;

		HiveParserASTNode KW_LATERAL65_tree=null;
		HiveParserASTNode KW_VIEW66_tree=null;
		HiveParserASTNode KW_OUTER67_tree=null;
		HiveParserASTNode KW_AS70_tree=null;
		HiveParserASTNode COMMA72_tree=null;
		HiveParserASTNode KW_LATERAL74_tree=null;
		HiveParserASTNode KW_VIEW75_tree=null;
		HiveParserASTNode KW_AS78_tree=null;
		HiveParserASTNode COMMA80_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_KW_VIEW=new RewriteRuleTokenStream(adaptor,"token KW_VIEW");
		RewriteRuleTokenStream stream_KW_OUTER=new RewriteRuleTokenStream(adaptor,"token KW_OUTER");
		RewriteRuleTokenStream stream_KW_AS=new RewriteRuleTokenStream(adaptor,"token KW_AS");
		RewriteRuleTokenStream stream_KW_LATERAL=new RewriteRuleTokenStream(adaptor,"token KW_LATERAL");
		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");
		RewriteRuleSubtreeStream stream_function=new RewriteRuleSubtreeStream(adaptor,"rule function");
		RewriteRuleSubtreeStream stream_tableAlias=new RewriteRuleSubtreeStream(adaptor,"rule tableAlias");

		gParent.pushMsg("lateral view", state); 
		try {
			// FromClauseASTParser.g:159:2: ( ( KW_LATERAL KW_VIEW KW_OUTER )=> KW_LATERAL KW_VIEW KW_OUTER function tableAlias ( KW_AS identifier ( ( COMMA )=> COMMA identifier )* )? -> ^( TOK_LATERAL_VIEW_OUTER ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) ) ) | KW_LATERAL KW_VIEW function tableAlias ( KW_AS identifier ( ( COMMA )=> COMMA identifier )* )? -> ^( TOK_LATERAL_VIEW ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) ) ) )
			int alt23=2;
			int LA23_0 = input.LA(1);
			if ( (LA23_0==KW_LATERAL) ) {
				int LA23_1 = input.LA(2);
				if ( (LA23_1==KW_VIEW) ) {
					int LA23_2 = input.LA(3);
					if ( (LA23_2==KW_OUTER) && (synpred2_FromClauseASTParser())) {
						alt23=1;
					}
					else if ( (LA23_2==Identifier||(LA23_2 >= KW_ABORT && LA23_2 <= KW_AFTER)||LA23_2==KW_ANALYZE||(LA23_2 >= KW_ARCHIVE && LA23_2 <= KW_ARRAY)||LA23_2==KW_ASC||(LA23_2 >= KW_AUTOCOMMIT && LA23_2 <= KW_BEFORE)||(LA23_2 >= KW_BIGINT && LA23_2 <= KW_BOOLEAN)||(LA23_2 >= KW_BUCKET && LA23_2 <= KW_BUCKETS)||(LA23_2 >= KW_CACHE && LA23_2 <= KW_CASCADE)||LA23_2==KW_CHANGE||(LA23_2 >= KW_CLUSTER && LA23_2 <= KW_COLLECTION)||(LA23_2 >= KW_COLUMNS && LA23_2 <= KW_COMMENT)||(LA23_2 >= KW_COMPACT && LA23_2 <= KW_CONCATENATE)||LA23_2==KW_CONTINUE||(LA23_2 >= KW_CURRENT_DATE && LA23_2 <= KW_CURRENT_TIMESTAMP)||LA23_2==KW_DATA||(LA23_2 >= KW_DATABASES && LA23_2 <= KW_DBPROPERTIES)||(LA23_2 >= KW_DEFERRED && LA23_2 <= KW_DEFINED)||(LA23_2 >= KW_DELIMITED && LA23_2 <= KW_DESC)||(LA23_2 >= KW_DETAIL && LA23_2 <= KW_DISABLE)||(LA23_2 >= KW_DISTRIBUTE && LA23_2 <= KW_DOW)||(LA23_2 >= KW_DUMP && LA23_2 <= KW_ELEM_TYPE)||LA23_2==KW_ENABLE||LA23_2==KW_ESCAPED||LA23_2==KW_EXCLUSIVE||(LA23_2 >= KW_EXPLAIN && LA23_2 <= KW_EXPRESSION)||(LA23_2 >= KW_FIELDS && LA23_2 <= KW_FLOAT)||(LA23_2 >= KW_FORMAT && LA23_2 <= KW_FORMATTED)||LA23_2==KW_FUNCTIONS||LA23_2==KW_GROUPING||(LA23_2 >= KW_HOUR && LA23_2 <= KW_IF)||(LA23_2 >= KW_INDEX && LA23_2 <= KW_INDEXES)||(LA23_2 >= KW_INPATH && LA23_2 <= KW_INPUTFORMAT)||LA23_2==KW_INT||(LA23_2 >= KW_ISOLATION && LA23_2 <= KW_JAR)||(LA23_2 >= KW_KEY && LA23_2 <= KW_LAST)||LA23_2==KW_LEVEL||(LA23_2 >= KW_LIMIT && LA23_2 <= KW_LOAD)||(LA23_2 >= KW_LOCATION && LA23_2 <= KW_LONG)||(LA23_2 >= KW_MAP && LA23_2 <= KW_MATERIALIZED)||LA23_2==KW_METADATA||(LA23_2 >= KW_MINUTE && LA23_2 <= KW_MONTH)||LA23_2==KW_MSCK||(LA23_2 >= KW_NORELY && LA23_2 <= KW_NOSCAN)||LA23_2==KW_NOVALIDATE||LA23_2==KW_NULLS||LA23_2==KW_OFFSET||(LA23_2 >= KW_OPERATOR && LA23_2 <= KW_OPTION)||(LA23_2 >= KW_OUTPUTDRIVER && LA23_2 <= KW_OUTPUTFORMAT)||(LA23_2 >= KW_OVERWRITE && LA23_2 <= KW_OWNER)||(LA23_2 >= KW_PARTITIONED && LA23_2 <= KW_PARTITIONS)||LA23_2==KW_PLUS||LA23_2==KW_PRETTY||LA23_2==KW_PRINCIPALS||(LA23_2 >= KW_PURGE && LA23_2 <= KW_QUARTER)||LA23_2==KW_READ||(LA23_2 >= KW_REBUILD && LA23_2 <= KW_RECORDWRITER)||(LA23_2 >= KW_RELOAD && LA23_2 <= KW_RESTRICT)||LA23_2==KW_REWRITE||(LA23_2 >= KW_ROLE && LA23_2 <= KW_ROLES)||(LA23_2 >= KW_SCHEMA && LA23_2 <= KW_SECOND)||(LA23_2 >= KW_SEMI && LA23_2 <= KW_SERVER)||(LA23_2 >= KW_SETS && LA23_2 <= KW_SSL)||(LA23_2 >= KW_STATISTICS && LA23_2 <= KW_SUMMARY)||LA23_2==KW_TABLES||(LA23_2 >= KW_TBLPROPERTIES && LA23_2 <= KW_TERMINATED)||(LA23_2 >= KW_TIMESTAMP && LA23_2 <= KW_TINYINT)||(LA23_2 >= KW_TOUCH && LA23_2 <= KW_TRANSACTIONS)||LA23_2==KW_UNARCHIVE||LA23_2==KW_UNDO||LA23_2==KW_UNIONTYPE||(LA23_2 >= KW_UNLOCK && LA23_2 <= KW_UNSIGNED)||(LA23_2 >= KW_URI && LA23_2 <= KW_USE)||(LA23_2 >= KW_UTC && LA23_2 <= KW_VALIDATE)||LA23_2==KW_VALUE_TYPE||(LA23_2 >= KW_VECTORIZATION && LA23_2 <= KW_WEEK)||LA23_2==KW_WHILE||(LA23_2 >= KW_WORK && LA23_2 <= KW_YEAR)||LA23_2==KW_BATCH||LA23_2==KW_DAYOFWEEK||LA23_2==KW_HOLD_DDLTIME||LA23_2==KW_IGNORE||LA23_2==KW_NO_DROP||LA23_2==KW_OFFLINE||LA23_2==KW_PROTECTION||LA23_2==KW_READONLY) ) {
						alt23=2;
					}

					else {
						if (state.backtracking>0) {state.failed=true; return retval;}
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 23, 2, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 23, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 23, 0, input);
				throw nvae;
			}

			switch (alt23) {
				case 1 :
					// FromClauseASTParser.g:160:2: ( KW_LATERAL KW_VIEW KW_OUTER )=> KW_LATERAL KW_VIEW KW_OUTER function tableAlias ( KW_AS identifier ( ( COMMA )=> COMMA identifier )* )?
					{
					KW_LATERAL65=(Token)match(input,KW_LATERAL,FOLLOW_KW_LATERAL_in_lateralView890); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_LATERAL.add(KW_LATERAL65);

					KW_VIEW66=(Token)match(input,KW_VIEW,FOLLOW_KW_VIEW_in_lateralView892); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_VIEW.add(KW_VIEW66);

					KW_OUTER67=(Token)match(input,KW_OUTER,FOLLOW_KW_OUTER_in_lateralView894); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_OUTER.add(KW_OUTER67);

					pushFollow(FOLLOW_function_in_lateralView896);
					function68=gHiveASTParser.function();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_function.add(function68.getTree());
					pushFollow(FOLLOW_tableAlias_in_lateralView898);
					tableAlias69=tableAlias();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_tableAlias.add(tableAlias69.getTree());
					// FromClauseASTParser.g:160:83: ( KW_AS identifier ( ( COMMA )=> COMMA identifier )* )?
					int alt20=2;
					int LA20_0 = input.LA(1);
					if ( (LA20_0==KW_AS) ) {
						alt20=1;
					}
					switch (alt20) {
						case 1 :
							// FromClauseASTParser.g:160:84: KW_AS identifier ( ( COMMA )=> COMMA identifier )*
							{
							KW_AS70=(Token)match(input,KW_AS,FOLLOW_KW_AS_in_lateralView901); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_AS.add(KW_AS70);

							pushFollow(FOLLOW_identifier_in_lateralView903);
							identifier71=gHiveASTParser.identifier();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) stream_identifier.add(identifier71.getTree());
							// FromClauseASTParser.g:160:101: ( ( COMMA )=> COMMA identifier )*
							loop19:
							while (true) {
								int alt19=2;
								alt19 = dfa19.predict(input);
								switch (alt19) {
								case 1 :
									// FromClauseASTParser.g:160:102: ( COMMA )=> COMMA identifier
									{
									COMMA72=(Token)match(input,COMMA,FOLLOW_COMMA_in_lateralView911); if (state.failed) return retval; 
									if ( state.backtracking==0 ) stream_COMMA.add(COMMA72);

									pushFollow(FOLLOW_identifier_in_lateralView913);
									identifier73=gHiveASTParser.identifier();
									state._fsp--;
									if (state.failed) return retval;
									if ( state.backtracking==0 ) stream_identifier.add(identifier73.getTree());
									}
									break;

								default :
									break loop19;
								}
							}

							}
							break;

					}

					// AST REWRITE
					// elements: identifier, function, tableAlias
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 161:2: -> ^( TOK_LATERAL_VIEW_OUTER ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) ) )
					{
						// FromClauseASTParser.g:161:5: ^( TOK_LATERAL_VIEW_OUTER ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) ) )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_LATERAL_VIEW_OUTER, "TOK_LATERAL_VIEW_OUTER"), root_1);
						// FromClauseASTParser.g:161:30: ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) )
						{
						HiveParserASTNode root_2 = (HiveParserASTNode)adaptor.nil();
						root_2 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_SELECT, "TOK_SELECT"), root_2);
						// FromClauseASTParser.g:161:43: ^( TOK_SELEXPR function ( identifier )* tableAlias )
						{
						HiveParserASTNode root_3 = (HiveParserASTNode)adaptor.nil();
						root_3 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_SELEXPR, "TOK_SELEXPR"), root_3);
						adaptor.addChild(root_3, stream_function.nextTree());
						// FromClauseASTParser.g:161:66: ( identifier )*
						while ( stream_identifier.hasNext() ) {
							adaptor.addChild(root_3, stream_identifier.nextTree());
						}
						stream_identifier.reset();

						adaptor.addChild(root_3, stream_tableAlias.nextTree());
						adaptor.addChild(root_2, root_3);
						}

						adaptor.addChild(root_1, root_2);
						}

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;
				case 2 :
					// FromClauseASTParser.g:163:2: KW_LATERAL KW_VIEW function tableAlias ( KW_AS identifier ( ( COMMA )=> COMMA identifier )* )?
					{
					KW_LATERAL74=(Token)match(input,KW_LATERAL,FOLLOW_KW_LATERAL_in_lateralView945); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_LATERAL.add(KW_LATERAL74);

					KW_VIEW75=(Token)match(input,KW_VIEW,FOLLOW_KW_VIEW_in_lateralView947); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_VIEW.add(KW_VIEW75);

					pushFollow(FOLLOW_function_in_lateralView949);
					function76=gHiveASTParser.function();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_function.add(function76.getTree());
					pushFollow(FOLLOW_tableAlias_in_lateralView951);
					tableAlias77=tableAlias();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_tableAlias.add(tableAlias77.getTree());
					// FromClauseASTParser.g:163:41: ( KW_AS identifier ( ( COMMA )=> COMMA identifier )* )?
					int alt22=2;
					int LA22_0 = input.LA(1);
					if ( (LA22_0==KW_AS) ) {
						alt22=1;
					}
					switch (alt22) {
						case 1 :
							// FromClauseASTParser.g:163:42: KW_AS identifier ( ( COMMA )=> COMMA identifier )*
							{
							KW_AS78=(Token)match(input,KW_AS,FOLLOW_KW_AS_in_lateralView954); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_AS.add(KW_AS78);

							pushFollow(FOLLOW_identifier_in_lateralView956);
							identifier79=gHiveASTParser.identifier();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) stream_identifier.add(identifier79.getTree());
							// FromClauseASTParser.g:163:59: ( ( COMMA )=> COMMA identifier )*
							loop21:
							while (true) {
								int alt21=2;
								alt21 = dfa21.predict(input);
								switch (alt21) {
								case 1 :
									// FromClauseASTParser.g:163:60: ( COMMA )=> COMMA identifier
									{
									COMMA80=(Token)match(input,COMMA,FOLLOW_COMMA_in_lateralView964); if (state.failed) return retval; 
									if ( state.backtracking==0 ) stream_COMMA.add(COMMA80);

									pushFollow(FOLLOW_identifier_in_lateralView966);
									identifier81=gHiveASTParser.identifier();
									state._fsp--;
									if (state.failed) return retval;
									if ( state.backtracking==0 ) stream_identifier.add(identifier81.getTree());
									}
									break;

								default :
									break loop21;
								}
							}

							}
							break;

					}

					// AST REWRITE
					// elements: function, tableAlias, identifier
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 164:2: -> ^( TOK_LATERAL_VIEW ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) ) )
					{
						// FromClauseASTParser.g:164:5: ^( TOK_LATERAL_VIEW ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) ) )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_LATERAL_VIEW, "TOK_LATERAL_VIEW"), root_1);
						// FromClauseASTParser.g:164:24: ^( TOK_SELECT ^( TOK_SELEXPR function ( identifier )* tableAlias ) )
						{
						HiveParserASTNode root_2 = (HiveParserASTNode)adaptor.nil();
						root_2 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_SELECT, "TOK_SELECT"), root_2);
						// FromClauseASTParser.g:164:37: ^( TOK_SELEXPR function ( identifier )* tableAlias )
						{
						HiveParserASTNode root_3 = (HiveParserASTNode)adaptor.nil();
						root_3 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_SELEXPR, "TOK_SELEXPR"), root_3);
						adaptor.addChild(root_3, stream_function.nextTree());
						// FromClauseASTParser.g:164:60: ( identifier )*
						while ( stream_identifier.hasNext() ) {
							adaptor.addChild(root_3, stream_identifier.nextTree());
						}
						stream_identifier.reset();

						adaptor.addChild(root_3, stream_tableAlias.nextTree());
						adaptor.addChild(root_2, root_3);
						}

						adaptor.addChild(root_1, root_2);
						}

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) {gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "lateralView"


	public static class tableAlias_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "tableAlias"
	// FromClauseASTParser.g:167:1: tableAlias : identifier -> ^( TOK_TABALIAS identifier ) ;
	public final HiveASTParser_FromClauseASTParser.tableAlias_return tableAlias() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.tableAlias_return retval = new HiveASTParser_FromClauseASTParser.tableAlias_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		ParserRuleReturnScope identifier82 =null;

		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");

		gParent.pushMsg("table alias", state); 
		try {
			// FromClauseASTParser.g:170:5: ( identifier -> ^( TOK_TABALIAS identifier ) )
			// FromClauseASTParser.g:171:5: identifier
			{
			pushFollow(FOLLOW_identifier_in_tableAlias1020);
			identifier82=gHiveASTParser.identifier();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_identifier.add(identifier82.getTree());
			// AST REWRITE
			// elements: identifier
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 171:16: -> ^( TOK_TABALIAS identifier )
			{
				// FromClauseASTParser.g:171:19: ^( TOK_TABALIAS identifier )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABALIAS, "TOK_TABALIAS"), root_1);
				adaptor.addChild(root_1, stream_identifier.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) {gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "tableAlias"


	public static class tableBucketSample_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "tableBucketSample"
	// FromClauseASTParser.g:174:1: tableBucketSample : KW_TABLESAMPLE LPAREN KW_BUCKET (numerator= Number ) KW_OUT KW_OF (denominator= Number ) ( KW_ON expr+= expression ( COMMA expr+= expression )* )? RPAREN -> ^( TOK_TABLEBUCKETSAMPLE $numerator $denominator ( $expr)* ) ;
	public final HiveASTParser_FromClauseASTParser.tableBucketSample_return tableBucketSample() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.tableBucketSample_return retval = new HiveASTParser_FromClauseASTParser.tableBucketSample_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token numerator=null;
		Token denominator=null;
		Token KW_TABLESAMPLE83=null;
		Token LPAREN84=null;
		Token KW_BUCKET85=null;
		Token KW_OUT86=null;
		Token KW_OF87=null;
		Token KW_ON88=null;
		Token COMMA89=null;
		Token RPAREN90=null;
		List<Object> list_expr=null;
		RuleReturnScope expr = null;
		HiveParserASTNode numerator_tree=null;
		HiveParserASTNode denominator_tree=null;
		HiveParserASTNode KW_TABLESAMPLE83_tree=null;
		HiveParserASTNode LPAREN84_tree=null;
		HiveParserASTNode KW_BUCKET85_tree=null;
		HiveParserASTNode KW_OUT86_tree=null;
		HiveParserASTNode KW_OF87_tree=null;
		HiveParserASTNode KW_ON88_tree=null;
		HiveParserASTNode COMMA89_tree=null;
		HiveParserASTNode RPAREN90_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_KW_TABLESAMPLE=new RewriteRuleTokenStream(adaptor,"token KW_TABLESAMPLE");
		RewriteRuleTokenStream stream_KW_OF=new RewriteRuleTokenStream(adaptor,"token KW_OF");
		RewriteRuleTokenStream stream_Number=new RewriteRuleTokenStream(adaptor,"token Number");
		RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
		RewriteRuleTokenStream stream_KW_OUT=new RewriteRuleTokenStream(adaptor,"token KW_OUT");
		RewriteRuleTokenStream stream_KW_ON=new RewriteRuleTokenStream(adaptor,"token KW_ON");
		RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
		RewriteRuleTokenStream stream_KW_BUCKET=new RewriteRuleTokenStream(adaptor,"token KW_BUCKET");
		RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");

		 gParent.pushMsg("table bucket sample specification", state); 
		try {
			// FromClauseASTParser.g:177:5: ( KW_TABLESAMPLE LPAREN KW_BUCKET (numerator= Number ) KW_OUT KW_OF (denominator= Number ) ( KW_ON expr+= expression ( COMMA expr+= expression )* )? RPAREN -> ^( TOK_TABLEBUCKETSAMPLE $numerator $denominator ( $expr)* ) )
			// FromClauseASTParser.g:178:5: KW_TABLESAMPLE LPAREN KW_BUCKET (numerator= Number ) KW_OUT KW_OF (denominator= Number ) ( KW_ON expr+= expression ( COMMA expr+= expression )* )? RPAREN
			{
			KW_TABLESAMPLE83=(Token)match(input,KW_TABLESAMPLE,FOLLOW_KW_TABLESAMPLE_in_tableBucketSample1059); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_KW_TABLESAMPLE.add(KW_TABLESAMPLE83);

			LPAREN84=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_tableBucketSample1061); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN84);

			KW_BUCKET85=(Token)match(input,KW_BUCKET,FOLLOW_KW_BUCKET_in_tableBucketSample1063); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_KW_BUCKET.add(KW_BUCKET85);

			// FromClauseASTParser.g:178:37: (numerator= Number )
			// FromClauseASTParser.g:178:38: numerator= Number
			{
			numerator=(Token)match(input,Number,FOLLOW_Number_in_tableBucketSample1068); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_Number.add(numerator);

			}

			KW_OUT86=(Token)match(input,KW_OUT,FOLLOW_KW_OUT_in_tableBucketSample1071); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_KW_OUT.add(KW_OUT86);

			KW_OF87=(Token)match(input,KW_OF,FOLLOW_KW_OF_in_tableBucketSample1073); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_KW_OF.add(KW_OF87);

			// FromClauseASTParser.g:178:69: (denominator= Number )
			// FromClauseASTParser.g:178:70: denominator= Number
			{
			denominator=(Token)match(input,Number,FOLLOW_Number_in_tableBucketSample1078); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_Number.add(denominator);

			}

			// FromClauseASTParser.g:178:90: ( KW_ON expr+= expression ( COMMA expr+= expression )* )?
			int alt25=2;
			int LA25_0 = input.LA(1);
			if ( (LA25_0==KW_ON) ) {
				alt25=1;
			}
			switch (alt25) {
				case 1 :
					// FromClauseASTParser.g:178:91: KW_ON expr+= expression ( COMMA expr+= expression )*
					{
					KW_ON88=(Token)match(input,KW_ON,FOLLOW_KW_ON_in_tableBucketSample1082); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_ON.add(KW_ON88);

					pushFollow(FOLLOW_expression_in_tableBucketSample1086);
					expr=gHiveASTParser.expression();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_expression.add(expr.getTree());
					if (list_expr==null) list_expr=new ArrayList<Object>();
					list_expr.add(expr.getTree());
					// FromClauseASTParser.g:178:114: ( COMMA expr+= expression )*
					loop24:
					while (true) {
						int alt24=2;
						int LA24_0 = input.LA(1);
						if ( (LA24_0==COMMA) ) {
							alt24=1;
						}

						switch (alt24) {
						case 1 :
							// FromClauseASTParser.g:178:115: COMMA expr+= expression
							{
							COMMA89=(Token)match(input,COMMA,FOLLOW_COMMA_in_tableBucketSample1089); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_COMMA.add(COMMA89);

							pushFollow(FOLLOW_expression_in_tableBucketSample1093);
							expr=gHiveASTParser.expression();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) stream_expression.add(expr.getTree());
							if (list_expr==null) list_expr=new ArrayList<Object>();
							list_expr.add(expr.getTree());
							}
							break;

						default :
							break loop24;
						}
					}

					}
					break;

			}

			RPAREN90=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_tableBucketSample1099); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN90);

			// AST REWRITE
			// elements: denominator, expr, numerator
			// token labels: denominator, numerator
			// rule labels: retval
			// token list labels: 
			// rule list labels: expr
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleTokenStream stream_denominator=new RewriteRuleTokenStream(adaptor,"token denominator",denominator);
			RewriteRuleTokenStream stream_numerator=new RewriteRuleTokenStream(adaptor,"token numerator",numerator);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);
			RewriteRuleSubtreeStream stream_expr=new RewriteRuleSubtreeStream(adaptor,"token expr",list_expr);
			root_0 = (HiveParserASTNode)adaptor.nil();
			// 178:149: -> ^( TOK_TABLEBUCKETSAMPLE $numerator $denominator ( $expr)* )
			{
				// FromClauseASTParser.g:178:152: ^( TOK_TABLEBUCKETSAMPLE $numerator $denominator ( $expr)* )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABLEBUCKETSAMPLE, "TOK_TABLEBUCKETSAMPLE"), root_1);
				adaptor.addChild(root_1, stream_numerator.nextNode());
				adaptor.addChild(root_1, stream_denominator.nextNode());
				// FromClauseASTParser.g:178:201: ( $expr)*
				while ( stream_expr.hasNext() ) {
					adaptor.addChild(root_1, stream_expr.nextTree());
				}
				stream_expr.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "tableBucketSample"


	public static class splitSample_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "splitSample"
	// FromClauseASTParser.g:181:1: splitSample : ( KW_TABLESAMPLE LPAREN (numerator= Number ) (percent= KW_PERCENT | KW_ROWS ) RPAREN -> {percent != null}? ^( TOK_TABLESPLITSAMPLE TOK_PERCENT $numerator) -> ^( TOK_TABLESPLITSAMPLE TOK_ROWCOUNT $numerator) | KW_TABLESAMPLE LPAREN (numerator= ByteLengthLiteral ) RPAREN -> ^( TOK_TABLESPLITSAMPLE TOK_LENGTH $numerator) );
	public final HiveASTParser_FromClauseASTParser.splitSample_return splitSample() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.splitSample_return retval = new HiveASTParser_FromClauseASTParser.splitSample_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token numerator=null;
		Token percent=null;
		Token KW_TABLESAMPLE91=null;
		Token LPAREN92=null;
		Token KW_ROWS93=null;
		Token RPAREN94=null;
		Token KW_TABLESAMPLE95=null;
		Token LPAREN96=null;
		Token RPAREN97=null;

		HiveParserASTNode numerator_tree=null;
		HiveParserASTNode percent_tree=null;
		HiveParserASTNode KW_TABLESAMPLE91_tree=null;
		HiveParserASTNode LPAREN92_tree=null;
		HiveParserASTNode KW_ROWS93_tree=null;
		HiveParserASTNode RPAREN94_tree=null;
		HiveParserASTNode KW_TABLESAMPLE95_tree=null;
		HiveParserASTNode LPAREN96_tree=null;
		HiveParserASTNode RPAREN97_tree=null;
		RewriteRuleTokenStream stream_KW_TABLESAMPLE=new RewriteRuleTokenStream(adaptor,"token KW_TABLESAMPLE");
		RewriteRuleTokenStream stream_Number=new RewriteRuleTokenStream(adaptor,"token Number");
		RewriteRuleTokenStream stream_KW_ROWS=new RewriteRuleTokenStream(adaptor,"token KW_ROWS");
		RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
		RewriteRuleTokenStream stream_KW_PERCENT=new RewriteRuleTokenStream(adaptor,"token KW_PERCENT");
		RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
		RewriteRuleTokenStream stream_ByteLengthLiteral=new RewriteRuleTokenStream(adaptor,"token ByteLengthLiteral");

		 gParent.pushMsg("table split sample specification", state); 
		try {
			// FromClauseASTParser.g:184:5: ( KW_TABLESAMPLE LPAREN (numerator= Number ) (percent= KW_PERCENT | KW_ROWS ) RPAREN -> {percent != null}? ^( TOK_TABLESPLITSAMPLE TOK_PERCENT $numerator) -> ^( TOK_TABLESPLITSAMPLE TOK_ROWCOUNT $numerator) | KW_TABLESAMPLE LPAREN (numerator= ByteLengthLiteral ) RPAREN -> ^( TOK_TABLESPLITSAMPLE TOK_LENGTH $numerator) )
			int alt27=2;
			int LA27_0 = input.LA(1);
			if ( (LA27_0==KW_TABLESAMPLE) ) {
				int LA27_1 = input.LA(2);
				if ( (LA27_1==LPAREN) ) {
					int LA27_2 = input.LA(3);
					if ( (LA27_2==Number) ) {
						alt27=1;
					}
					else if ( (LA27_2==ByteLengthLiteral) ) {
						alt27=2;
					}

					else {
						if (state.backtracking>0) {state.failed=true; return retval;}
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 27, 2, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 27, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 27, 0, input);
				throw nvae;
			}

			switch (alt27) {
				case 1 :
					// FromClauseASTParser.g:185:5: KW_TABLESAMPLE LPAREN (numerator= Number ) (percent= KW_PERCENT | KW_ROWS ) RPAREN
					{
					KW_TABLESAMPLE91=(Token)match(input,KW_TABLESAMPLE,FOLLOW_KW_TABLESAMPLE_in_splitSample1146); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_TABLESAMPLE.add(KW_TABLESAMPLE91);

					LPAREN92=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_splitSample1148); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN92);

					// FromClauseASTParser.g:185:28: (numerator= Number )
					// FromClauseASTParser.g:185:29: numerator= Number
					{
					numerator=(Token)match(input,Number,FOLLOW_Number_in_splitSample1154); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_Number.add(numerator);

					}

					// FromClauseASTParser.g:185:47: (percent= KW_PERCENT | KW_ROWS )
					int alt26=2;
					int LA26_0 = input.LA(1);
					if ( (LA26_0==KW_PERCENT) ) {
						alt26=1;
					}
					else if ( (LA26_0==KW_ROWS) ) {
						alt26=2;
					}

					else {
						if (state.backtracking>0) {state.failed=true; return retval;}
						NoViableAltException nvae =
							new NoViableAltException("", 26, 0, input);
						throw nvae;
					}

					switch (alt26) {
						case 1 :
							// FromClauseASTParser.g:185:48: percent= KW_PERCENT
							{
							percent=(Token)match(input,KW_PERCENT,FOLLOW_KW_PERCENT_in_splitSample1160); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_PERCENT.add(percent);

							}
							break;
						case 2 :
							// FromClauseASTParser.g:185:67: KW_ROWS
							{
							KW_ROWS93=(Token)match(input,KW_ROWS,FOLLOW_KW_ROWS_in_splitSample1162); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_ROWS.add(KW_ROWS93);

							}
							break;

					}

					RPAREN94=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_splitSample1165); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN94);

					// AST REWRITE
					// elements: numerator, numerator
					// token labels: numerator
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleTokenStream stream_numerator=new RewriteRuleTokenStream(adaptor,"token numerator",numerator);
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 186:5: -> {percent != null}? ^( TOK_TABLESPLITSAMPLE TOK_PERCENT $numerator)
					if (percent != null) {
						// FromClauseASTParser.g:186:27: ^( TOK_TABLESPLITSAMPLE TOK_PERCENT $numerator)
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABLESPLITSAMPLE, "TOK_TABLESPLITSAMPLE"), root_1);
						adaptor.addChild(root_1, (HiveParserASTNode)adaptor.create(TOK_PERCENT, "TOK_PERCENT"));
						adaptor.addChild(root_1, stream_numerator.nextNode());
						adaptor.addChild(root_0, root_1);
						}

					}

					else // 187:5: -> ^( TOK_TABLESPLITSAMPLE TOK_ROWCOUNT $numerator)
					{
						// FromClauseASTParser.g:187:8: ^( TOK_TABLESPLITSAMPLE TOK_ROWCOUNT $numerator)
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABLESPLITSAMPLE, "TOK_TABLESPLITSAMPLE"), root_1);
						adaptor.addChild(root_1, (HiveParserASTNode)adaptor.create(TOK_ROWCOUNT, "TOK_ROWCOUNT"));
						adaptor.addChild(root_1, stream_numerator.nextNode());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;
				case 2 :
					// FromClauseASTParser.g:189:5: KW_TABLESAMPLE LPAREN (numerator= ByteLengthLiteral ) RPAREN
					{
					KW_TABLESAMPLE95=(Token)match(input,KW_TABLESAMPLE,FOLLOW_KW_TABLESAMPLE_in_splitSample1209); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_TABLESAMPLE.add(KW_TABLESAMPLE95);

					LPAREN96=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_splitSample1211); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN96);

					// FromClauseASTParser.g:189:28: (numerator= ByteLengthLiteral )
					// FromClauseASTParser.g:189:29: numerator= ByteLengthLiteral
					{
					numerator=(Token)match(input,ByteLengthLiteral,FOLLOW_ByteLengthLiteral_in_splitSample1217); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_ByteLengthLiteral.add(numerator);

					}

					RPAREN97=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_splitSample1220); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN97);

					// AST REWRITE
					// elements: numerator
					// token labels: numerator
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleTokenStream stream_numerator=new RewriteRuleTokenStream(adaptor,"token numerator",numerator);
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 190:5: -> ^( TOK_TABLESPLITSAMPLE TOK_LENGTH $numerator)
					{
						// FromClauseASTParser.g:190:8: ^( TOK_TABLESPLITSAMPLE TOK_LENGTH $numerator)
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABLESPLITSAMPLE, "TOK_TABLESPLITSAMPLE"), root_1);
						adaptor.addChild(root_1, (HiveParserASTNode)adaptor.create(TOK_LENGTH, "TOK_LENGTH"));
						adaptor.addChild(root_1, stream_numerator.nextNode());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "splitSample"


	public static class tableSample_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "tableSample"
	// FromClauseASTParser.g:193:1: tableSample : ( tableBucketSample | splitSample );
	public final HiveASTParser_FromClauseASTParser.tableSample_return tableSample() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.tableSample_return retval = new HiveASTParser_FromClauseASTParser.tableSample_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		ParserRuleReturnScope tableBucketSample98 =null;
		ParserRuleReturnScope splitSample99 =null;


		 gParent.pushMsg("table sample specification", state); 
		try {
			// FromClauseASTParser.g:196:5: ( tableBucketSample | splitSample )
			int alt28=2;
			int LA28_0 = input.LA(1);
			if ( (LA28_0==KW_TABLESAMPLE) ) {
				int LA28_1 = input.LA(2);
				if ( (LA28_1==LPAREN) ) {
					int LA28_2 = input.LA(3);
					if ( (LA28_2==KW_BUCKET) ) {
						alt28=1;
					}
					else if ( (LA28_2==ByteLengthLiteral||LA28_2==Number) ) {
						alt28=2;
					}

					else {
						if (state.backtracking>0) {state.failed=true; return retval;}
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 28, 2, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 28, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 28, 0, input);
				throw nvae;
			}

			switch (alt28) {
				case 1 :
					// FromClauseASTParser.g:197:5: tableBucketSample
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_tableBucketSample_in_tableSample1266);
					tableBucketSample98=tableBucketSample();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, tableBucketSample98.getTree());

					}
					break;
				case 2 :
					// FromClauseASTParser.g:198:5: splitSample
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_splitSample_in_tableSample1274);
					splitSample99=splitSample();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, splitSample99.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "tableSample"


	public static class tableSource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "tableSource"
	// FromClauseASTParser.g:201:1: tableSource : tabname= tableName (props= tableProperties )? (ts= tableSample )? ( ( KW_AS )? alias= identifier )? -> ^( TOK_TABREF $tabname ( $props)? ( $ts)? ( $alias)? ) ;
	public final HiveASTParser_FromClauseASTParser.tableSource_return tableSource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.tableSource_return retval = new HiveASTParser_FromClauseASTParser.tableSource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_AS100=null;
		ParserRuleReturnScope tabname =null;
		ParserRuleReturnScope props =null;
		ParserRuleReturnScope ts =null;
		ParserRuleReturnScope alias =null;

		HiveParserASTNode KW_AS100_tree=null;
		RewriteRuleTokenStream stream_KW_AS=new RewriteRuleTokenStream(adaptor,"token KW_AS");
		RewriteRuleSubtreeStream stream_tableSample=new RewriteRuleSubtreeStream(adaptor,"rule tableSample");
		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");
		RewriteRuleSubtreeStream stream_tableProperties=new RewriteRuleSubtreeStream(adaptor,"rule tableProperties");
		RewriteRuleSubtreeStream stream_tableName=new RewriteRuleSubtreeStream(adaptor,"rule tableName");

		 gParent.pushMsg("table source", state); 
		try {
			// FromClauseASTParser.g:204:5: (tabname= tableName (props= tableProperties )? (ts= tableSample )? ( ( KW_AS )? alias= identifier )? -> ^( TOK_TABREF $tabname ( $props)? ( $ts)? ( $alias)? ) )
			// FromClauseASTParser.g:204:7: tabname= tableName (props= tableProperties )? (ts= tableSample )? ( ( KW_AS )? alias= identifier )?
			{
			pushFollow(FOLLOW_tableName_in_tableSource1303);
			tabname=tableName();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_tableName.add(tabname.getTree());
			// FromClauseASTParser.g:204:30: (props= tableProperties )?
			int alt29=2;
			int LA29_0 = input.LA(1);
			if ( (LA29_0==LPAREN) ) {
				alt29=1;
			}
			switch (alt29) {
				case 1 :
					// FromClauseASTParser.g:204:30: props= tableProperties
					{
					pushFollow(FOLLOW_tableProperties_in_tableSource1307);
					props=gHiveASTParser.tableProperties();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_tableProperties.add(props.getTree());
					}
					break;

			}

			// FromClauseASTParser.g:204:50: (ts= tableSample )?
			int alt30=2;
			int LA30_0 = input.LA(1);
			if ( (LA30_0==KW_TABLESAMPLE) ) {
				alt30=1;
			}
			switch (alt30) {
				case 1 :
					// FromClauseASTParser.g:204:50: ts= tableSample
					{
					pushFollow(FOLLOW_tableSample_in_tableSource1312);
					ts=tableSample();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_tableSample.add(ts.getTree());
					}
					break;

			}

			// FromClauseASTParser.g:204:64: ( ( KW_AS )? alias= identifier )?
			int alt32=2;
			alt32 = dfa32.predict(input);
			switch (alt32) {
				case 1 :
					// FromClauseASTParser.g:204:65: ( KW_AS )? alias= identifier
					{
					// FromClauseASTParser.g:204:65: ( KW_AS )?
					int alt31=2;
					int LA31_0 = input.LA(1);
					if ( (LA31_0==KW_AS) ) {
						alt31=1;
					}
					switch (alt31) {
						case 1 :
							// FromClauseASTParser.g:204:65: KW_AS
							{
							KW_AS100=(Token)match(input,KW_AS,FOLLOW_KW_AS_in_tableSource1316); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_AS.add(KW_AS100);

							}
							break;

					}

					pushFollow(FOLLOW_identifier_in_tableSource1321);
					alias=gHiveASTParser.identifier();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_identifier.add(alias.getTree());
					}
					break;

			}

			// AST REWRITE
			// elements: props, ts, alias, tabname
			// token labels: 
			// rule labels: tabname, alias, retval, props, ts
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_tabname=new RewriteRuleSubtreeStream(adaptor,"rule tabname",tabname!=null?tabname.getTree():null);
			RewriteRuleSubtreeStream stream_alias=new RewriteRuleSubtreeStream(adaptor,"rule alias",alias!=null?alias.getTree():null);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);
			RewriteRuleSubtreeStream stream_props=new RewriteRuleSubtreeStream(adaptor,"rule props",props!=null?props.getTree():null);
			RewriteRuleSubtreeStream stream_ts=new RewriteRuleSubtreeStream(adaptor,"rule ts",ts!=null?ts.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 205:5: -> ^( TOK_TABREF $tabname ( $props)? ( $ts)? ( $alias)? )
			{
				// FromClauseASTParser.g:205:8: ^( TOK_TABREF $tabname ( $props)? ( $ts)? ( $alias)? )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABREF, "TOK_TABREF"), root_1);
				adaptor.addChild(root_1, stream_tabname.nextTree());
				// FromClauseASTParser.g:205:31: ( $props)?
				if ( stream_props.hasNext() ) {
					adaptor.addChild(root_1, stream_props.nextTree());
				}
				stream_props.reset();

				// FromClauseASTParser.g:205:39: ( $ts)?
				if ( stream_ts.hasNext() ) {
					adaptor.addChild(root_1, stream_ts.nextTree());
				}
				stream_ts.reset();

				// FromClauseASTParser.g:205:44: ( $alias)?
				if ( stream_alias.hasNext() ) {
					adaptor.addChild(root_1, stream_alias.nextTree());
				}
				stream_alias.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "tableSource"


	public static class uniqueJoinTableSource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "uniqueJoinTableSource"
	// FromClauseASTParser.g:208:1: uniqueJoinTableSource : tabname= tableName (ts= tableSample )? ( ( KW_AS )? alias= identifier )? -> ^( TOK_TABREF $tabname ( $ts)? ( $alias)? ) ;
	public final HiveASTParser_FromClauseASTParser.uniqueJoinTableSource_return uniqueJoinTableSource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.uniqueJoinTableSource_return retval = new HiveASTParser_FromClauseASTParser.uniqueJoinTableSource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_AS101=null;
		ParserRuleReturnScope tabname =null;
		ParserRuleReturnScope ts =null;
		ParserRuleReturnScope alias =null;

		HiveParserASTNode KW_AS101_tree=null;
		RewriteRuleTokenStream stream_KW_AS=new RewriteRuleTokenStream(adaptor,"token KW_AS");
		RewriteRuleSubtreeStream stream_tableSample=new RewriteRuleSubtreeStream(adaptor,"rule tableSample");
		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");
		RewriteRuleSubtreeStream stream_tableName=new RewriteRuleSubtreeStream(adaptor,"rule tableName");

		 gParent.pushMsg("unique join table source", state); 
		try {
			// FromClauseASTParser.g:211:5: (tabname= tableName (ts= tableSample )? ( ( KW_AS )? alias= identifier )? -> ^( TOK_TABREF $tabname ( $ts)? ( $alias)? ) )
			// FromClauseASTParser.g:211:7: tabname= tableName (ts= tableSample )? ( ( KW_AS )? alias= identifier )?
			{
			pushFollow(FOLLOW_tableName_in_uniqueJoinTableSource1377);
			tabname=tableName();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_tableName.add(tabname.getTree());
			// FromClauseASTParser.g:211:27: (ts= tableSample )?
			int alt33=2;
			int LA33_0 = input.LA(1);
			if ( (LA33_0==KW_TABLESAMPLE) ) {
				alt33=1;
			}
			switch (alt33) {
				case 1 :
					// FromClauseASTParser.g:211:27: ts= tableSample
					{
					pushFollow(FOLLOW_tableSample_in_uniqueJoinTableSource1381);
					ts=tableSample();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_tableSample.add(ts.getTree());
					}
					break;

			}

			// FromClauseASTParser.g:211:41: ( ( KW_AS )? alias= identifier )?
			int alt35=2;
			int LA35_0 = input.LA(1);
			if ( (LA35_0==Identifier||(LA35_0 >= KW_ABORT && LA35_0 <= KW_AFTER)||LA35_0==KW_ANALYZE||LA35_0==KW_ARCHIVE||(LA35_0 >= KW_AS && LA35_0 <= KW_ASC)||(LA35_0 >= KW_AUTOCOMMIT && LA35_0 <= KW_BEFORE)||(LA35_0 >= KW_BUCKET && LA35_0 <= KW_BUCKETS)||(LA35_0 >= KW_CACHE && LA35_0 <= KW_CASCADE)||LA35_0==KW_CHANGE||(LA35_0 >= KW_CLUSTER && LA35_0 <= KW_COLLECTION)||(LA35_0 >= KW_COLUMNS && LA35_0 <= KW_COMMENT)||(LA35_0 >= KW_COMPACT && LA35_0 <= KW_CONCATENATE)||LA35_0==KW_CONTINUE||LA35_0==KW_DATA||LA35_0==KW_DATABASES||(LA35_0 >= KW_DATETIME && LA35_0 <= KW_DBPROPERTIES)||(LA35_0 >= KW_DEFERRED && LA35_0 <= KW_DEFINED)||(LA35_0 >= KW_DELIMITED && LA35_0 <= KW_DESC)||(LA35_0 >= KW_DETAIL && LA35_0 <= KW_DISABLE)||LA35_0==KW_DISTRIBUTE||LA35_0==KW_DOW||(LA35_0 >= KW_DUMP && LA35_0 <= KW_ELEM_TYPE)||LA35_0==KW_ENABLE||LA35_0==KW_ESCAPED||LA35_0==KW_EXCLUSIVE||(LA35_0 >= KW_EXPLAIN && LA35_0 <= KW_EXPRESSION)||(LA35_0 >= KW_FIELDS && LA35_0 <= KW_FIRST)||(LA35_0 >= KW_FORMAT && LA35_0 <= KW_FORMATTED)||LA35_0==KW_FUNCTIONS||(LA35_0 >= KW_HOUR && LA35_0 <= KW_IDXPROPERTIES)||(LA35_0 >= KW_INDEX && LA35_0 <= KW_INDEXES)||(LA35_0 >= KW_INPATH && LA35_0 <= KW_INPUTFORMAT)||(LA35_0 >= KW_ISOLATION && LA35_0 <= KW_JAR)||(LA35_0 >= KW_KEY && LA35_0 <= KW_LAST)||LA35_0==KW_LEVEL||(LA35_0 >= KW_LIMIT && LA35_0 <= KW_LOAD)||(LA35_0 >= KW_LOCATION && LA35_0 <= KW_LONG)||(LA35_0 >= KW_MAPJOIN && LA35_0 <= KW_MATERIALIZED)||LA35_0==KW_METADATA||(LA35_0 >= KW_MINUTE && LA35_0 <= KW_MONTH)||LA35_0==KW_MSCK||(LA35_0 >= KW_NORELY && LA35_0 <= KW_NOSCAN)||LA35_0==KW_NOVALIDATE||LA35_0==KW_NULLS||LA35_0==KW_OFFSET||(LA35_0 >= KW_OPERATOR && LA35_0 <= KW_OPTION)||(LA35_0 >= KW_OUTPUTDRIVER && LA35_0 <= KW_OUTPUTFORMAT)||(LA35_0 >= KW_OVERWRITE && LA35_0 <= KW_OWNER)||(LA35_0 >= KW_PARTITIONED && LA35_0 <= KW_PARTITIONS)||LA35_0==KW_PLUS||LA35_0==KW_PRETTY||LA35_0==KW_PRINCIPALS||(LA35_0 >= KW_PURGE && LA35_0 <= KW_QUARTER)||LA35_0==KW_READ||(LA35_0 >= KW_REBUILD && LA35_0 <= KW_RECORDWRITER)||(LA35_0 >= KW_RELOAD && LA35_0 <= KW_RESTRICT)||LA35_0==KW_REWRITE||(LA35_0 >= KW_ROLE && LA35_0 <= KW_ROLES)||(LA35_0 >= KW_SCHEMA && LA35_0 <= KW_SECOND)||(LA35_0 >= KW_SEMI && LA35_0 <= KW_SERVER)||(LA35_0 >= KW_SETS && LA35_0 <= KW_SKEWED)||(LA35_0 >= KW_SNAPSHOT && LA35_0 <= KW_SSL)||(LA35_0 >= KW_STATISTICS && LA35_0 <= KW_SUMMARY)||LA35_0==KW_TABLES||(LA35_0 >= KW_TBLPROPERTIES && LA35_0 <= KW_TERMINATED)||LA35_0==KW_TINYINT||(LA35_0 >= KW_TOUCH && LA35_0 <= KW_TRANSACTIONS)||LA35_0==KW_UNARCHIVE||LA35_0==KW_UNDO||LA35_0==KW_UNIONTYPE||(LA35_0 >= KW_UNLOCK && LA35_0 <= KW_UNSIGNED)||(LA35_0 >= KW_URI && LA35_0 <= KW_USE)||(LA35_0 >= KW_UTC && LA35_0 <= KW_VALIDATE)||LA35_0==KW_VALUE_TYPE||(LA35_0 >= KW_VECTORIZATION && LA35_0 <= KW_WEEK)||LA35_0==KW_WHILE||(LA35_0 >= KW_WORK && LA35_0 <= KW_YEAR)||LA35_0==KW_BATCH||LA35_0==KW_DAYOFWEEK||LA35_0==KW_HOLD_DDLTIME||LA35_0==KW_IGNORE||LA35_0==KW_NO_DROP||LA35_0==KW_OFFLINE||LA35_0==KW_PROTECTION||LA35_0==KW_READONLY) ) {
				alt35=1;
			}
			switch (alt35) {
				case 1 :
					// FromClauseASTParser.g:211:42: ( KW_AS )? alias= identifier
					{
					// FromClauseASTParser.g:211:42: ( KW_AS )?
					int alt34=2;
					int LA34_0 = input.LA(1);
					if ( (LA34_0==KW_AS) ) {
						alt34=1;
					}
					switch (alt34) {
						case 1 :
							// FromClauseASTParser.g:211:42: KW_AS
							{
							KW_AS101=(Token)match(input,KW_AS,FOLLOW_KW_AS_in_uniqueJoinTableSource1385); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_KW_AS.add(KW_AS101);

							}
							break;

					}

					pushFollow(FOLLOW_identifier_in_uniqueJoinTableSource1390);
					alias=gHiveASTParser.identifier();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_identifier.add(alias.getTree());
					}
					break;

			}

			// AST REWRITE
			// elements: ts, alias, tabname
			// token labels: 
			// rule labels: tabname, alias, retval, ts
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_tabname=new RewriteRuleSubtreeStream(adaptor,"rule tabname",tabname!=null?tabname.getTree():null);
			RewriteRuleSubtreeStream stream_alias=new RewriteRuleSubtreeStream(adaptor,"rule alias",alias!=null?alias.getTree():null);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);
			RewriteRuleSubtreeStream stream_ts=new RewriteRuleSubtreeStream(adaptor,"rule ts",ts!=null?ts.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 212:5: -> ^( TOK_TABREF $tabname ( $ts)? ( $alias)? )
			{
				// FromClauseASTParser.g:212:8: ^( TOK_TABREF $tabname ( $ts)? ( $alias)? )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABREF, "TOK_TABREF"), root_1);
				adaptor.addChild(root_1, stream_tabname.nextTree());
				// FromClauseASTParser.g:212:31: ( $ts)?
				if ( stream_ts.hasNext() ) {
					adaptor.addChild(root_1, stream_ts.nextTree());
				}
				stream_ts.reset();

				// FromClauseASTParser.g:212:36: ( $alias)?
				if ( stream_alias.hasNext() ) {
					adaptor.addChild(root_1, stream_alias.nextTree());
				}
				stream_alias.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "uniqueJoinTableSource"


	public static class tableName_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "tableName"
	// FromClauseASTParser.g:215:1: tableName : (db= identifier DOT tab= identifier -> ^( TOK_TABNAME $db $tab) |tab= identifier -> ^( TOK_TABNAME $tab) );
	public final HiveASTParser_FromClauseASTParser.tableName_return tableName() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.tableName_return retval = new HiveASTParser_FromClauseASTParser.tableName_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token DOT102=null;
		ParserRuleReturnScope db =null;
		ParserRuleReturnScope tab =null;

		HiveParserASTNode DOT102_tree=null;
		RewriteRuleTokenStream stream_DOT=new RewriteRuleTokenStream(adaptor,"token DOT");
		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");

		 gParent.pushMsg("table name", state); 
		try {
			// FromClauseASTParser.g:218:5: (db= identifier DOT tab= identifier -> ^( TOK_TABNAME $db $tab) |tab= identifier -> ^( TOK_TABNAME $tab) )
			int alt36=2;
			alt36 = dfa36.predict(input);
			switch (alt36) {
				case 1 :
					// FromClauseASTParser.g:219:5: db= identifier DOT tab= identifier
					{
					pushFollow(FOLLOW_identifier_in_tableName1446);
					db=gHiveASTParser.identifier();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_identifier.add(db.getTree());
					DOT102=(Token)match(input,DOT,FOLLOW_DOT_in_tableName1448); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_DOT.add(DOT102);

					pushFollow(FOLLOW_identifier_in_tableName1452);
					tab=gHiveASTParser.identifier();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_identifier.add(tab.getTree());
					// AST REWRITE
					// elements: tab, db
					// token labels: 
					// rule labels: tab, db, retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_tab=new RewriteRuleSubtreeStream(adaptor,"rule tab",tab!=null?tab.getTree():null);
					RewriteRuleSubtreeStream stream_db=new RewriteRuleSubtreeStream(adaptor,"rule db",db!=null?db.getTree():null);
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 220:5: -> ^( TOK_TABNAME $db $tab)
					{
						// FromClauseASTParser.g:220:8: ^( TOK_TABNAME $db $tab)
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABNAME, "TOK_TABNAME"), root_1);
						adaptor.addChild(root_1, stream_db.nextTree());
						adaptor.addChild(root_1, stream_tab.nextTree());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;
				case 2 :
					// FromClauseASTParser.g:222:5: tab= identifier
					{
					pushFollow(FOLLOW_identifier_in_tableName1482);
					tab=gHiveASTParser.identifier();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_identifier.add(tab.getTree());
					// AST REWRITE
					// elements: tab
					// token labels: 
					// rule labels: tab, retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_tab=new RewriteRuleSubtreeStream(adaptor,"rule tab",tab!=null?tab.getTree():null);
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 223:5: -> ^( TOK_TABNAME $tab)
					{
						// FromClauseASTParser.g:223:8: ^( TOK_TABNAME $tab)
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABNAME, "TOK_TABNAME"), root_1);
						adaptor.addChild(root_1, stream_tab.nextTree());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "tableName"


	public static class viewName_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "viewName"
	// FromClauseASTParser.g:226:1: viewName : (db= identifier DOT )? view= identifier -> ^( TOK_TABNAME ( $db)? $view) ;
	public final HiveASTParser_FromClauseASTParser.viewName_return viewName() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.viewName_return retval = new HiveASTParser_FromClauseASTParser.viewName_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token DOT103=null;
		ParserRuleReturnScope db =null;
		ParserRuleReturnScope view =null;

		HiveParserASTNode DOT103_tree=null;
		RewriteRuleTokenStream stream_DOT=new RewriteRuleTokenStream(adaptor,"token DOT");
		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");

		 gParent.pushMsg("view name", state); 
		try {
			// FromClauseASTParser.g:229:5: ( (db= identifier DOT )? view= identifier -> ^( TOK_TABNAME ( $db)? $view) )
			// FromClauseASTParser.g:230:5: (db= identifier DOT )? view= identifier
			{
			// FromClauseASTParser.g:230:5: (db= identifier DOT )?
			int alt37=2;
			int LA37_0 = input.LA(1);
			if ( (LA37_0==Identifier) ) {
				int LA37_1 = input.LA(2);
				if ( (LA37_1==DOT) ) {
					alt37=1;
				}
			}
			else if ( ((LA37_0 >= KW_ABORT && LA37_0 <= KW_AFTER)||LA37_0==KW_ANALYZE||LA37_0==KW_ARCHIVE||LA37_0==KW_ASC||(LA37_0 >= KW_AUTOCOMMIT && LA37_0 <= KW_BEFORE)||(LA37_0 >= KW_BUCKET && LA37_0 <= KW_BUCKETS)||(LA37_0 >= KW_CACHE && LA37_0 <= KW_CASCADE)||LA37_0==KW_CHANGE||(LA37_0 >= KW_CLUSTER && LA37_0 <= KW_COLLECTION)||(LA37_0 >= KW_COLUMNS && LA37_0 <= KW_COMMENT)||(LA37_0 >= KW_COMPACT && LA37_0 <= KW_CONCATENATE)||LA37_0==KW_CONTINUE||LA37_0==KW_DATA||LA37_0==KW_DATABASES||(LA37_0 >= KW_DATETIME && LA37_0 <= KW_DBPROPERTIES)||(LA37_0 >= KW_DEFERRED && LA37_0 <= KW_DEFINED)||(LA37_0 >= KW_DELIMITED && LA37_0 <= KW_DESC)||(LA37_0 >= KW_DETAIL && LA37_0 <= KW_DISABLE)||LA37_0==KW_DISTRIBUTE||LA37_0==KW_DOW||(LA37_0 >= KW_DUMP && LA37_0 <= KW_ELEM_TYPE)||LA37_0==KW_ENABLE||LA37_0==KW_ESCAPED||LA37_0==KW_EXCLUSIVE||(LA37_0 >= KW_EXPLAIN && LA37_0 <= KW_EXPRESSION)||(LA37_0 >= KW_FIELDS && LA37_0 <= KW_FIRST)||(LA37_0 >= KW_FORMAT && LA37_0 <= KW_FORMATTED)||LA37_0==KW_FUNCTIONS||(LA37_0 >= KW_HOUR && LA37_0 <= KW_IDXPROPERTIES)||(LA37_0 >= KW_INDEX && LA37_0 <= KW_INDEXES)||(LA37_0 >= KW_INPATH && LA37_0 <= KW_INPUTFORMAT)||(LA37_0 >= KW_ISOLATION && LA37_0 <= KW_JAR)||(LA37_0 >= KW_KEY && LA37_0 <= KW_LAST)||LA37_0==KW_LEVEL||(LA37_0 >= KW_LIMIT && LA37_0 <= KW_LOAD)||(LA37_0 >= KW_LOCATION && LA37_0 <= KW_LONG)||(LA37_0 >= KW_MAPJOIN && LA37_0 <= KW_MATERIALIZED)||LA37_0==KW_METADATA||(LA37_0 >= KW_MINUTE && LA37_0 <= KW_MONTH)||LA37_0==KW_MSCK||(LA37_0 >= KW_NORELY && LA37_0 <= KW_NOSCAN)||LA37_0==KW_NOVALIDATE||LA37_0==KW_NULLS||LA37_0==KW_OFFSET||(LA37_0 >= KW_OPERATOR && LA37_0 <= KW_OPTION)||(LA37_0 >= KW_OUTPUTDRIVER && LA37_0 <= KW_OUTPUTFORMAT)||(LA37_0 >= KW_OVERWRITE && LA37_0 <= KW_OWNER)||(LA37_0 >= KW_PARTITIONED && LA37_0 <= KW_PARTITIONS)||LA37_0==KW_PLUS||LA37_0==KW_PRETTY||LA37_0==KW_PRINCIPALS||(LA37_0 >= KW_PURGE && LA37_0 <= KW_QUARTER)||LA37_0==KW_READ||(LA37_0 >= KW_REBUILD && LA37_0 <= KW_RECORDWRITER)||(LA37_0 >= KW_RELOAD && LA37_0 <= KW_RESTRICT)||LA37_0==KW_REWRITE||(LA37_0 >= KW_ROLE && LA37_0 <= KW_ROLES)||(LA37_0 >= KW_SCHEMA && LA37_0 <= KW_SECOND)||(LA37_0 >= KW_SEMI && LA37_0 <= KW_SERVER)||(LA37_0 >= KW_SETS && LA37_0 <= KW_SKEWED)||(LA37_0 >= KW_SNAPSHOT && LA37_0 <= KW_SSL)||(LA37_0 >= KW_STATISTICS && LA37_0 <= KW_SUMMARY)||LA37_0==KW_TABLES||(LA37_0 >= KW_TBLPROPERTIES && LA37_0 <= KW_TERMINATED)||LA37_0==KW_TINYINT||(LA37_0 >= KW_TOUCH && LA37_0 <= KW_TRANSACTIONS)||LA37_0==KW_UNARCHIVE||LA37_0==KW_UNDO||LA37_0==KW_UNIONTYPE||(LA37_0 >= KW_UNLOCK && LA37_0 <= KW_UNSIGNED)||(LA37_0 >= KW_URI && LA37_0 <= KW_USE)||(LA37_0 >= KW_UTC && LA37_0 <= KW_VALIDATE)||LA37_0==KW_VALUE_TYPE||(LA37_0 >= KW_VECTORIZATION && LA37_0 <= KW_WEEK)||LA37_0==KW_WHILE||(LA37_0 >= KW_WORK && LA37_0 <= KW_YEAR)||LA37_0==KW_BATCH||LA37_0==KW_DAYOFWEEK||LA37_0==KW_HOLD_DDLTIME||LA37_0==KW_IGNORE||LA37_0==KW_NO_DROP||LA37_0==KW_OFFLINE||LA37_0==KW_PROTECTION||LA37_0==KW_READONLY) ) {
				int LA37_2 = input.LA(2);
				if ( (LA37_2==DOT) ) {
					alt37=1;
				}
			}
			switch (alt37) {
				case 1 :
					// FromClauseASTParser.g:230:6: db= identifier DOT
					{
					pushFollow(FOLLOW_identifier_in_viewName1529);
					db=gHiveASTParser.identifier();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_identifier.add(db.getTree());
					DOT103=(Token)match(input,DOT,FOLLOW_DOT_in_viewName1531); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_DOT.add(DOT103);

					}
					break;

			}

			pushFollow(FOLLOW_identifier_in_viewName1537);
			view=gHiveASTParser.identifier();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_identifier.add(view.getTree());
			// AST REWRITE
			// elements: view, db
			// token labels: 
			// rule labels: view, db, retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_view=new RewriteRuleSubtreeStream(adaptor,"rule view",view!=null?view.getTree():null);
			RewriteRuleSubtreeStream stream_db=new RewriteRuleSubtreeStream(adaptor,"rule db",db!=null?db.getTree():null);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 231:5: -> ^( TOK_TABNAME ( $db)? $view)
			{
				// FromClauseASTParser.g:231:8: ^( TOK_TABNAME ( $db)? $view)
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABNAME, "TOK_TABNAME"), root_1);
				// FromClauseASTParser.g:231:23: ( $db)?
				if ( stream_db.hasNext() ) {
					adaptor.addChild(root_1, stream_db.nextTree());
				}
				stream_db.reset();

				adaptor.addChild(root_1, stream_view.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "viewName"


	public static class subQuerySource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "subQuerySource"
	// FromClauseASTParser.g:234:1: subQuerySource : LPAREN queryStatementExpression RPAREN ( KW_AS )? identifier -> ^( TOK_SUBQUERY queryStatementExpression identifier ) ;
	public final HiveASTParser_FromClauseASTParser.subQuerySource_return subQuerySource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.subQuerySource_return retval = new HiveASTParser_FromClauseASTParser.subQuerySource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token LPAREN104=null;
		Token RPAREN106=null;
		Token KW_AS107=null;
		ParserRuleReturnScope queryStatementExpression105 =null;
		ParserRuleReturnScope identifier108 =null;

		HiveParserASTNode LPAREN104_tree=null;
		HiveParserASTNode RPAREN106_tree=null;
		HiveParserASTNode KW_AS107_tree=null;
		RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
		RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
		RewriteRuleTokenStream stream_KW_AS=new RewriteRuleTokenStream(adaptor,"token KW_AS");
		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");
		RewriteRuleSubtreeStream stream_queryStatementExpression=new RewriteRuleSubtreeStream(adaptor,"rule queryStatementExpression");

		 gParent.pushMsg("subquery source", state); 
		try {
			// FromClauseASTParser.g:237:5: ( LPAREN queryStatementExpression RPAREN ( KW_AS )? identifier -> ^( TOK_SUBQUERY queryStatementExpression identifier ) )
			// FromClauseASTParser.g:238:5: LPAREN queryStatementExpression RPAREN ( KW_AS )? identifier
			{
			LPAREN104=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_subQuerySource1585); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN104);

			pushFollow(FOLLOW_queryStatementExpression_in_subQuerySource1587);
			queryStatementExpression105=gHiveASTParser.queryStatementExpression();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_queryStatementExpression.add(queryStatementExpression105.getTree());
			RPAREN106=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_subQuerySource1589); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN106);

			// FromClauseASTParser.g:238:44: ( KW_AS )?
			int alt38=2;
			int LA38_0 = input.LA(1);
			if ( (LA38_0==KW_AS) ) {
				alt38=1;
			}
			switch (alt38) {
				case 1 :
					// FromClauseASTParser.g:238:44: KW_AS
					{
					KW_AS107=(Token)match(input,KW_AS,FOLLOW_KW_AS_in_subQuerySource1591); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_AS.add(KW_AS107);

					}
					break;

			}

			pushFollow(FOLLOW_identifier_in_subQuerySource1594);
			identifier108=gHiveASTParser.identifier();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_identifier.add(identifier108.getTree());
			// AST REWRITE
			// elements: queryStatementExpression, identifier
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 238:62: -> ^( TOK_SUBQUERY queryStatementExpression identifier )
			{
				// FromClauseASTParser.g:238:65: ^( TOK_SUBQUERY queryStatementExpression identifier )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_SUBQUERY, "TOK_SUBQUERY"), root_1);
				adaptor.addChild(root_1, stream_queryStatementExpression.nextTree());
				adaptor.addChild(root_1, stream_identifier.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "subQuerySource"


	public static class partitioningSpec_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "partitioningSpec"
	// FromClauseASTParser.g:242:1: partitioningSpec : ( partitionByClause ( orderByClause )? -> ^( TOK_PARTITIONINGSPEC partitionByClause ( orderByClause )? ) | orderByClause -> ^( TOK_PARTITIONINGSPEC orderByClause ) | distributeByClause ( sortByClause )? -> ^( TOK_PARTITIONINGSPEC distributeByClause ( sortByClause )? ) | sortByClause -> ^( TOK_PARTITIONINGSPEC sortByClause ) | clusterByClause -> ^( TOK_PARTITIONINGSPEC clusterByClause ) );
	public final HiveASTParser_FromClauseASTParser.partitioningSpec_return partitioningSpec() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.partitioningSpec_return retval = new HiveASTParser_FromClauseASTParser.partitioningSpec_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		ParserRuleReturnScope partitionByClause109 =null;
		ParserRuleReturnScope orderByClause110 =null;
		ParserRuleReturnScope orderByClause111 =null;
		ParserRuleReturnScope distributeByClause112 =null;
		ParserRuleReturnScope sortByClause113 =null;
		ParserRuleReturnScope sortByClause114 =null;
		ParserRuleReturnScope clusterByClause115 =null;

		RewriteRuleSubtreeStream stream_clusterByClause=new RewriteRuleSubtreeStream(adaptor,"rule clusterByClause");
		RewriteRuleSubtreeStream stream_sortByClause=new RewriteRuleSubtreeStream(adaptor,"rule sortByClause");
		RewriteRuleSubtreeStream stream_partitionByClause=new RewriteRuleSubtreeStream(adaptor,"rule partitionByClause");
		RewriteRuleSubtreeStream stream_distributeByClause=new RewriteRuleSubtreeStream(adaptor,"rule distributeByClause");
		RewriteRuleSubtreeStream stream_orderByClause=new RewriteRuleSubtreeStream(adaptor,"rule orderByClause");

		 gParent.pushMsg("partitioningSpec clause", state); 
		try {
			// FromClauseASTParser.g:245:4: ( partitionByClause ( orderByClause )? -> ^( TOK_PARTITIONINGSPEC partitionByClause ( orderByClause )? ) | orderByClause -> ^( TOK_PARTITIONINGSPEC orderByClause ) | distributeByClause ( sortByClause )? -> ^( TOK_PARTITIONINGSPEC distributeByClause ( sortByClause )? ) | sortByClause -> ^( TOK_PARTITIONINGSPEC sortByClause ) | clusterByClause -> ^( TOK_PARTITIONINGSPEC clusterByClause ) )
			int alt41=5;
			switch ( input.LA(1) ) {
			case KW_PARTITION:
				{
				alt41=1;
				}
				break;
			case KW_ORDER:
				{
				alt41=2;
				}
				break;
			case KW_DISTRIBUTE:
				{
				alt41=3;
				}
				break;
			case KW_SORT:
				{
				alt41=4;
				}
				break;
			case KW_CLUSTER:
				{
				alt41=5;
				}
				break;
			default:
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 41, 0, input);
				throw nvae;
			}
			switch (alt41) {
				case 1 :
					// FromClauseASTParser.g:246:4: partitionByClause ( orderByClause )?
					{
					pushFollow(FOLLOW_partitionByClause_in_partitioningSpec1635);
					partitionByClause109=gHiveASTParser.partitionByClause();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_partitionByClause.add(partitionByClause109.getTree());
					// FromClauseASTParser.g:246:22: ( orderByClause )?
					int alt39=2;
					int LA39_0 = input.LA(1);
					if ( (LA39_0==KW_ORDER) ) {
						alt39=1;
					}
					switch (alt39) {
						case 1 :
							// FromClauseASTParser.g:246:22: orderByClause
							{
							pushFollow(FOLLOW_orderByClause_in_partitioningSpec1637);
							orderByClause110=gHiveASTParser.orderByClause();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) stream_orderByClause.add(orderByClause110.getTree());
							}
							break;

					}

					// AST REWRITE
					// elements: partitionByClause, orderByClause
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 246:37: -> ^( TOK_PARTITIONINGSPEC partitionByClause ( orderByClause )? )
					{
						// FromClauseASTParser.g:246:40: ^( TOK_PARTITIONINGSPEC partitionByClause ( orderByClause )? )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_PARTITIONINGSPEC, "TOK_PARTITIONINGSPEC"), root_1);
						adaptor.addChild(root_1, stream_partitionByClause.nextTree());
						// FromClauseASTParser.g:246:81: ( orderByClause )?
						if ( stream_orderByClause.hasNext() ) {
							adaptor.addChild(root_1, stream_orderByClause.nextTree());
						}
						stream_orderByClause.reset();

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;
				case 2 :
					// FromClauseASTParser.g:247:4: orderByClause
					{
					pushFollow(FOLLOW_orderByClause_in_partitioningSpec1656);
					orderByClause111=gHiveASTParser.orderByClause();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_orderByClause.add(orderByClause111.getTree());
					// AST REWRITE
					// elements: orderByClause
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 247:18: -> ^( TOK_PARTITIONINGSPEC orderByClause )
					{
						// FromClauseASTParser.g:247:21: ^( TOK_PARTITIONINGSPEC orderByClause )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_PARTITIONINGSPEC, "TOK_PARTITIONINGSPEC"), root_1);
						adaptor.addChild(root_1, stream_orderByClause.nextTree());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;
				case 3 :
					// FromClauseASTParser.g:248:4: distributeByClause ( sortByClause )?
					{
					pushFollow(FOLLOW_distributeByClause_in_partitioningSpec1671);
					distributeByClause112=gHiveASTParser.distributeByClause();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_distributeByClause.add(distributeByClause112.getTree());
					// FromClauseASTParser.g:248:23: ( sortByClause )?
					int alt40=2;
					int LA40_0 = input.LA(1);
					if ( (LA40_0==KW_SORT) ) {
						alt40=1;
					}
					switch (alt40) {
						case 1 :
							// FromClauseASTParser.g:248:23: sortByClause
							{
							pushFollow(FOLLOW_sortByClause_in_partitioningSpec1673);
							sortByClause113=gHiveASTParser.sortByClause();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) stream_sortByClause.add(sortByClause113.getTree());
							}
							break;

					}

					// AST REWRITE
					// elements: sortByClause, distributeByClause
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 248:37: -> ^( TOK_PARTITIONINGSPEC distributeByClause ( sortByClause )? )
					{
						// FromClauseASTParser.g:248:40: ^( TOK_PARTITIONINGSPEC distributeByClause ( sortByClause )? )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_PARTITIONINGSPEC, "TOK_PARTITIONINGSPEC"), root_1);
						adaptor.addChild(root_1, stream_distributeByClause.nextTree());
						// FromClauseASTParser.g:248:82: ( sortByClause )?
						if ( stream_sortByClause.hasNext() ) {
							adaptor.addChild(root_1, stream_sortByClause.nextTree());
						}
						stream_sortByClause.reset();

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;
				case 4 :
					// FromClauseASTParser.g:249:4: sortByClause
					{
					pushFollow(FOLLOW_sortByClause_in_partitioningSpec1692);
					sortByClause114=gHiveASTParser.sortByClause();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_sortByClause.add(sortByClause114.getTree());
					// AST REWRITE
					// elements: sortByClause
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 249:17: -> ^( TOK_PARTITIONINGSPEC sortByClause )
					{
						// FromClauseASTParser.g:249:20: ^( TOK_PARTITIONINGSPEC sortByClause )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_PARTITIONINGSPEC, "TOK_PARTITIONINGSPEC"), root_1);
						adaptor.addChild(root_1, stream_sortByClause.nextTree());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;
				case 5 :
					// FromClauseASTParser.g:250:4: clusterByClause
					{
					pushFollow(FOLLOW_clusterByClause_in_partitioningSpec1707);
					clusterByClause115=gHiveASTParser.clusterByClause();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_clusterByClause.add(clusterByClause115.getTree());
					// AST REWRITE
					// elements: clusterByClause
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					if ( state.backtracking==0 ) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (HiveParserASTNode)adaptor.nil();
					// 250:20: -> ^( TOK_PARTITIONINGSPEC clusterByClause )
					{
						// FromClauseASTParser.g:250:23: ^( TOK_PARTITIONINGSPEC clusterByClause )
						{
						HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
						root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_PARTITIONINGSPEC, "TOK_PARTITIONINGSPEC"), root_1);
						adaptor.addChild(root_1, stream_clusterByClause.nextTree());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;
					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "partitioningSpec"


	public static class partitionTableFunctionSource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "partitionTableFunctionSource"
	// FromClauseASTParser.g:253:1: partitionTableFunctionSource : ( subQuerySource | tableSource | partitionedTableFunction );
	public final HiveASTParser_FromClauseASTParser.partitionTableFunctionSource_return partitionTableFunctionSource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.partitionTableFunctionSource_return retval = new HiveASTParser_FromClauseASTParser.partitionTableFunctionSource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		ParserRuleReturnScope subQuerySource116 =null;
		ParserRuleReturnScope tableSource117 =null;
		ParserRuleReturnScope partitionedTableFunction118 =null;


		 gParent.pushMsg("partitionTableFunctionSource clause", state); 
		try {
			// FromClauseASTParser.g:256:4: ( subQuerySource | tableSource | partitionedTableFunction )
			int alt42=3;
			switch ( input.LA(1) ) {
			case LPAREN:
				{
				alt42=1;
				}
				break;
			case Identifier:
				{
				int LA42_2 = input.LA(2);
				if ( (LA42_2==LPAREN) ) {
					int LA42_4 = input.LA(3);
					if ( (LA42_4==KW_ON) ) {
						alt42=3;
					}
					else if ( (LA42_4==StringLiteral) ) {
						alt42=2;
					}

					else {
						if (state.backtracking>0) {state.failed=true; return retval;}
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 42, 4, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}
				else if ( (LA42_2==EOF||LA42_2==DOT||LA42_2==Identifier||(LA42_2 >= KW_ABORT && LA42_2 <= KW_AFTER)||LA42_2==KW_ANALYZE||LA42_2==KW_ARCHIVE||(LA42_2 >= KW_AS && LA42_2 <= KW_ASC)||(LA42_2 >= KW_AUTOCOMMIT && LA42_2 <= KW_BEFORE)||(LA42_2 >= KW_BUCKET && LA42_2 <= KW_BUCKETS)||(LA42_2 >= KW_CACHE && LA42_2 <= KW_CASCADE)||LA42_2==KW_CHANGE||(LA42_2 >= KW_CLUSTER && LA42_2 <= KW_COLLECTION)||(LA42_2 >= KW_COLUMNS && LA42_2 <= KW_COMMENT)||(LA42_2 >= KW_COMPACT && LA42_2 <= KW_CONCATENATE)||LA42_2==KW_CONTINUE||LA42_2==KW_DATA||LA42_2==KW_DATABASES||(LA42_2 >= KW_DATETIME && LA42_2 <= KW_DBPROPERTIES)||(LA42_2 >= KW_DEFERRED && LA42_2 <= KW_DEFINED)||(LA42_2 >= KW_DELIMITED && LA42_2 <= KW_DESC)||(LA42_2 >= KW_DETAIL && LA42_2 <= KW_DISABLE)||LA42_2==KW_DISTRIBUTE||LA42_2==KW_DOW||(LA42_2 >= KW_DUMP && LA42_2 <= KW_ELEM_TYPE)||LA42_2==KW_ENABLE||LA42_2==KW_ESCAPED||LA42_2==KW_EXCLUSIVE||(LA42_2 >= KW_EXPLAIN && LA42_2 <= KW_EXPRESSION)||(LA42_2 >= KW_FIELDS && LA42_2 <= KW_FIRST)||(LA42_2 >= KW_FORMAT && LA42_2 <= KW_FORMATTED)||LA42_2==KW_FUNCTIONS||(LA42_2 >= KW_HOUR && LA42_2 <= KW_IDXPROPERTIES)||(LA42_2 >= KW_INDEX && LA42_2 <= KW_INDEXES)||(LA42_2 >= KW_INPATH && LA42_2 <= KW_INPUTFORMAT)||(LA42_2 >= KW_ISOLATION && LA42_2 <= KW_JAR)||(LA42_2 >= KW_KEY && LA42_2 <= KW_LAST)||LA42_2==KW_LEVEL||(LA42_2 >= KW_LIMIT && LA42_2 <= KW_LOAD)||(LA42_2 >= KW_LOCATION && LA42_2 <= KW_LONG)||(LA42_2 >= KW_MAPJOIN && LA42_2 <= KW_MATERIALIZED)||LA42_2==KW_METADATA||(LA42_2 >= KW_MINUTE && LA42_2 <= KW_MONTH)||LA42_2==KW_MSCK||(LA42_2 >= KW_NORELY && LA42_2 <= KW_NOSCAN)||LA42_2==KW_NOVALIDATE||LA42_2==KW_NULLS||LA42_2==KW_OFFSET||(LA42_2 >= KW_OPERATOR && LA42_2 <= KW_OPTION)||LA42_2==KW_ORDER||(LA42_2 >= KW_OUTPUTDRIVER && LA42_2 <= KW_OUTPUTFORMAT)||(LA42_2 >= KW_OVERWRITE && LA42_2 <= KW_OWNER)||(LA42_2 >= KW_PARTITION && LA42_2 <= KW_PARTITIONS)||LA42_2==KW_PLUS||LA42_2==KW_PRETTY||LA42_2==KW_PRINCIPALS||(LA42_2 >= KW_PURGE && LA42_2 <= KW_QUARTER)||LA42_2==KW_READ||(LA42_2 >= KW_REBUILD && LA42_2 <= KW_RECORDWRITER)||(LA42_2 >= KW_RELOAD && LA42_2 <= KW_RESTRICT)||LA42_2==KW_REWRITE||(LA42_2 >= KW_ROLE && LA42_2 <= KW_ROLES)||(LA42_2 >= KW_SCHEMA && LA42_2 <= KW_SECOND)||(LA42_2 >= KW_SEMI && LA42_2 <= KW_SERVER)||(LA42_2 >= KW_SETS && LA42_2 <= KW_SKEWED)||(LA42_2 >= KW_SNAPSHOT && LA42_2 <= KW_SSL)||(LA42_2 >= KW_STATISTICS && LA42_2 <= KW_SUMMARY)||(LA42_2 >= KW_TABLES && LA42_2 <= KW_TERMINATED)||LA42_2==KW_TINYINT||(LA42_2 >= KW_TOUCH && LA42_2 <= KW_TRANSACTIONS)||LA42_2==KW_UNARCHIVE||LA42_2==KW_UNDO||LA42_2==KW_UNIONTYPE||(LA42_2 >= KW_UNLOCK && LA42_2 <= KW_UNSIGNED)||(LA42_2 >= KW_URI && LA42_2 <= KW_USE)||(LA42_2 >= KW_UTC && LA42_2 <= KW_VALIDATE)||LA42_2==KW_VALUE_TYPE||(LA42_2 >= KW_VECTORIZATION && LA42_2 <= KW_WEEK)||LA42_2==KW_WHILE||(LA42_2 >= KW_WORK && LA42_2 <= KW_YEAR)||LA42_2==RPAREN||LA42_2==KW_BATCH||LA42_2==KW_DAYOFWEEK||LA42_2==KW_HOLD_DDLTIME||LA42_2==KW_IGNORE||LA42_2==KW_NO_DROP||LA42_2==KW_OFFLINE||LA42_2==KW_PROTECTION||LA42_2==KW_READONLY) ) {
					alt42=2;
				}

				else {
					if (state.backtracking>0) {state.failed=true; return retval;}
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 42, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case KW_ABORT:
			case KW_ADD:
			case KW_ADMIN:
			case KW_AFTER:
			case KW_ANALYZE:
			case KW_ARCHIVE:
			case KW_ASC:
			case KW_AUTOCOMMIT:
			case KW_BEFORE:
			case KW_BUCKET:
			case KW_BUCKETS:
			case KW_CACHE:
			case KW_CASCADE:
			case KW_CHANGE:
			case KW_CLUSTER:
			case KW_CLUSTERED:
			case KW_CLUSTERSTATUS:
			case KW_COLLECTION:
			case KW_COLUMNS:
			case KW_COMMENT:
			case KW_COMPACT:
			case KW_COMPACTIONS:
			case KW_COMPUTE:
			case KW_CONCATENATE:
			case KW_CONTINUE:
			case KW_DATA:
			case KW_DATABASES:
			case KW_DATETIME:
			case KW_DAY:
			case KW_DBPROPERTIES:
			case KW_DEFERRED:
			case KW_DEFINED:
			case KW_DELIMITED:
			case KW_DEPENDENCY:
			case KW_DESC:
			case KW_DETAIL:
			case KW_DIRECTORIES:
			case KW_DIRECTORY:
			case KW_DISABLE:
			case KW_DISTRIBUTE:
			case KW_DOW:
			case KW_DUMP:
			case KW_ELEM_TYPE:
			case KW_ENABLE:
			case KW_ESCAPED:
			case KW_EXCLUSIVE:
			case KW_EXPLAIN:
			case KW_EXPORT:
			case KW_EXPRESSION:
			case KW_FIELDS:
			case KW_FILE:
			case KW_FILEFORMAT:
			case KW_FIRST:
			case KW_FORMAT:
			case KW_FORMATTED:
			case KW_FUNCTIONS:
			case KW_HOUR:
			case KW_IDXPROPERTIES:
			case KW_INDEX:
			case KW_INDEXES:
			case KW_INPATH:
			case KW_INPUTDRIVER:
			case KW_INPUTFORMAT:
			case KW_ISOLATION:
			case KW_ITEMS:
			case KW_JAR:
			case KW_KEY:
			case KW_KEYS:
			case KW_KEY_TYPE:
			case KW_LAST:
			case KW_LEVEL:
			case KW_LIMIT:
			case KW_LINES:
			case KW_LOAD:
			case KW_LOCATION:
			case KW_LOCK:
			case KW_LOCKS:
			case KW_LOGICAL:
			case KW_LONG:
			case KW_MAPJOIN:
			case KW_MATCHED:
			case KW_MATERIALIZED:
			case KW_METADATA:
			case KW_MINUTE:
			case KW_MONTH:
			case KW_MSCK:
			case KW_NORELY:
			case KW_NOSCAN:
			case KW_NOVALIDATE:
			case KW_NULLS:
			case KW_OFFSET:
			case KW_OPERATOR:
			case KW_OPTION:
			case KW_OUTPUTDRIVER:
			case KW_OUTPUTFORMAT:
			case KW_OVERWRITE:
			case KW_OWNER:
			case KW_PARTITIONED:
			case KW_PARTITIONS:
			case KW_PLUS:
			case KW_PRETTY:
			case KW_PRINCIPALS:
			case KW_PURGE:
			case KW_QUARTER:
			case KW_READ:
			case KW_REBUILD:
			case KW_RECORDREADER:
			case KW_RECORDWRITER:
			case KW_RELOAD:
			case KW_RELY:
			case KW_RENAME:
			case KW_REPAIR:
			case KW_REPL:
			case KW_REPLACE:
			case KW_REPLICATION:
			case KW_RESTRICT:
			case KW_REWRITE:
			case KW_ROLE:
			case KW_ROLES:
			case KW_SCHEMA:
			case KW_SCHEMAS:
			case KW_SECOND:
			case KW_SEMI:
			case KW_SERDE:
			case KW_SERDEPROPERTIES:
			case KW_SERVER:
			case KW_SETS:
			case KW_SHARED:
			case KW_SHOW:
			case KW_SHOW_DATABASE:
			case KW_SKEWED:
			case KW_SNAPSHOT:
			case KW_SORT:
			case KW_SORTED:
			case KW_SSL:
			case KW_STATISTICS:
			case KW_STATUS:
			case KW_STORED:
			case KW_STREAMTABLE:
			case KW_STRING:
			case KW_STRUCT:
			case KW_SUMMARY:
			case KW_TABLES:
			case KW_TBLPROPERTIES:
			case KW_TEMPORARY:
			case KW_TERMINATED:
			case KW_TINYINT:
			case KW_TOUCH:
			case KW_TRANSACTION:
			case KW_TRANSACTIONS:
			case KW_UNARCHIVE:
			case KW_UNDO:
			case KW_UNIONTYPE:
			case KW_UNLOCK:
			case KW_UNSET:
			case KW_UNSIGNED:
			case KW_URI:
			case KW_USE:
			case KW_UTC:
			case KW_UTCTIMESTAMP:
			case KW_VALIDATE:
			case KW_VALUE_TYPE:
			case KW_VECTORIZATION:
			case KW_VIEW:
			case KW_VIEWS:
			case KW_WAIT:
			case KW_WEEK:
			case KW_WHILE:
			case KW_WORK:
			case KW_WRITE:
			case KW_YEAR:
			case KW_BATCH:
			case KW_DAYOFWEEK:
			case KW_HOLD_DDLTIME:
			case KW_IGNORE:
			case KW_NO_DROP:
			case KW_OFFLINE:
			case KW_PROTECTION:
			case KW_READONLY:
				{
				alt42=2;
				}
				break;
			default:
				if (state.backtracking>0) {state.failed=true; return retval;}
				NoViableAltException nvae =
					new NoViableAltException("", 42, 0, input);
				throw nvae;
			}
			switch (alt42) {
				case 1 :
					// FromClauseASTParser.g:257:4: subQuerySource
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_subQuerySource_in_partitionTableFunctionSource1744);
					subQuerySource116=subQuerySource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, subQuerySource116.getTree());

					}
					break;
				case 2 :
					// FromClauseASTParser.g:258:4: tableSource
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_tableSource_in_partitionTableFunctionSource1751);
					tableSource117=tableSource();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, tableSource117.getTree());

					}
					break;
				case 3 :
					// FromClauseASTParser.g:259:4: partitionedTableFunction
					{
					root_0 = (HiveParserASTNode)adaptor.nil();


					pushFollow(FOLLOW_partitionedTableFunction_in_partitionTableFunctionSource1758);
					partitionedTableFunction118=partitionedTableFunction();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) adaptor.addChild(root_0, partitionedTableFunction118.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "partitionTableFunctionSource"


	public static class partitionedTableFunction_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "partitionedTableFunction"
	// FromClauseASTParser.g:262:1: partitionedTableFunction : name= Identifier LPAREN KW_ON ( ( partitionTableFunctionSource )=> (ptfsrc= partitionTableFunctionSource (spec= partitioningSpec )? ) ) ( ( Identifier LPAREN expression RPAREN )=> Identifier LPAREN expression RPAREN ( COMMA Identifier LPAREN expression RPAREN )* )? ( ( RPAREN )=> ( RPAREN ) ) ( ( Identifier )=>alias= Identifier )? -> ^( TOK_PTBLFUNCTION $name ( $alias)? $ptfsrc ( $spec)? ( expression )* ) ;
	public final HiveASTParser_FromClauseASTParser.partitionedTableFunction_return partitionedTableFunction() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.partitionedTableFunction_return retval = new HiveASTParser_FromClauseASTParser.partitionedTableFunction_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token name=null;
		Token alias=null;
		Token LPAREN119=null;
		Token KW_ON120=null;
		Token Identifier121=null;
		Token LPAREN122=null;
		Token RPAREN124=null;
		Token COMMA125=null;
		Token Identifier126=null;
		Token LPAREN127=null;
		Token RPAREN129=null;
		Token RPAREN130=null;
		ParserRuleReturnScope ptfsrc =null;
		ParserRuleReturnScope spec =null;
		ParserRuleReturnScope expression123 =null;
		ParserRuleReturnScope expression128 =null;

		HiveParserASTNode name_tree=null;
		HiveParserASTNode alias_tree=null;
		HiveParserASTNode LPAREN119_tree=null;
		HiveParserASTNode KW_ON120_tree=null;
		HiveParserASTNode Identifier121_tree=null;
		HiveParserASTNode LPAREN122_tree=null;
		HiveParserASTNode RPAREN124_tree=null;
		HiveParserASTNode COMMA125_tree=null;
		HiveParserASTNode Identifier126_tree=null;
		HiveParserASTNode LPAREN127_tree=null;
		HiveParserASTNode RPAREN129_tree=null;
		HiveParserASTNode RPAREN130_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_Identifier=new RewriteRuleTokenStream(adaptor,"token Identifier");
		RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
		RewriteRuleTokenStream stream_KW_ON=new RewriteRuleTokenStream(adaptor,"token KW_ON");
		RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
		RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
		RewriteRuleSubtreeStream stream_partitionTableFunctionSource=new RewriteRuleSubtreeStream(adaptor,"rule partitionTableFunctionSource");
		RewriteRuleSubtreeStream stream_partitioningSpec=new RewriteRuleSubtreeStream(adaptor,"rule partitioningSpec");

		 gParent.pushMsg("ptf clause", state); 
		try {
			// FromClauseASTParser.g:265:4: (name= Identifier LPAREN KW_ON ( ( partitionTableFunctionSource )=> (ptfsrc= partitionTableFunctionSource (spec= partitioningSpec )? ) ) ( ( Identifier LPAREN expression RPAREN )=> Identifier LPAREN expression RPAREN ( COMMA Identifier LPAREN expression RPAREN )* )? ( ( RPAREN )=> ( RPAREN ) ) ( ( Identifier )=>alias= Identifier )? -> ^( TOK_PTBLFUNCTION $name ( $alias)? $ptfsrc ( $spec)? ( expression )* ) )
			// FromClauseASTParser.g:266:4: name= Identifier LPAREN KW_ON ( ( partitionTableFunctionSource )=> (ptfsrc= partitionTableFunctionSource (spec= partitioningSpec )? ) ) ( ( Identifier LPAREN expression RPAREN )=> Identifier LPAREN expression RPAREN ( COMMA Identifier LPAREN expression RPAREN )* )? ( ( RPAREN )=> ( RPAREN ) ) ( ( Identifier )=>alias= Identifier )?
			{
			name=(Token)match(input,Identifier,FOLLOW_Identifier_in_partitionedTableFunction1789); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_Identifier.add(name);

			LPAREN119=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_partitionedTableFunction1791); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN119);

			KW_ON120=(Token)match(input,KW_ON,FOLLOW_KW_ON_in_partitionedTableFunction1793); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_KW_ON.add(KW_ON120);

			// FromClauseASTParser.g:267:4: ( ( partitionTableFunctionSource )=> (ptfsrc= partitionTableFunctionSource (spec= partitioningSpec )? ) )
			// FromClauseASTParser.g:267:5: ( partitionTableFunctionSource )=> (ptfsrc= partitionTableFunctionSource (spec= partitioningSpec )? )
			{
			// FromClauseASTParser.g:267:39: (ptfsrc= partitionTableFunctionSource (spec= partitioningSpec )? )
			// FromClauseASTParser.g:267:40: ptfsrc= partitionTableFunctionSource (spec= partitioningSpec )?
			{
			pushFollow(FOLLOW_partitionTableFunctionSource_in_partitionedTableFunction1809);
			ptfsrc=partitionTableFunctionSource();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_partitionTableFunctionSource.add(ptfsrc.getTree());
			// FromClauseASTParser.g:267:80: (spec= partitioningSpec )?
			int alt43=2;
			int LA43_0 = input.LA(1);
			if ( (LA43_0==KW_CLUSTER||LA43_0==KW_DISTRIBUTE||LA43_0==KW_ORDER||LA43_0==KW_PARTITION||LA43_0==KW_SORT) ) {
				alt43=1;
			}
			switch (alt43) {
				case 1 :
					// FromClauseASTParser.g:267:80: spec= partitioningSpec
					{
					pushFollow(FOLLOW_partitioningSpec_in_partitionedTableFunction1813);
					spec=partitioningSpec();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_partitioningSpec.add(spec.getTree());
					}
					break;

			}

			}

			}

			// FromClauseASTParser.g:268:4: ( ( Identifier LPAREN expression RPAREN )=> Identifier LPAREN expression RPAREN ( COMMA Identifier LPAREN expression RPAREN )* )?
			int alt45=2;
			int LA45_0 = input.LA(1);
			if ( (LA45_0==Identifier) && (synpred6_FromClauseASTParser())) {
				alt45=1;
			}
			switch (alt45) {
				case 1 :
					// FromClauseASTParser.g:268:5: ( Identifier LPAREN expression RPAREN )=> Identifier LPAREN expression RPAREN ( COMMA Identifier LPAREN expression RPAREN )*
					{
					Identifier121=(Token)match(input,Identifier,FOLLOW_Identifier_in_partitionedTableFunction1835); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_Identifier.add(Identifier121);

					LPAREN122=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_partitionedTableFunction1837); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN122);

					pushFollow(FOLLOW_expression_in_partitionedTableFunction1839);
					expression123=gHiveASTParser.expression();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_expression.add(expression123.getTree());
					RPAREN124=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_partitionedTableFunction1841); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN124);

					// FromClauseASTParser.g:268:83: ( COMMA Identifier LPAREN expression RPAREN )*
					loop44:
					while (true) {
						int alt44=2;
						int LA44_0 = input.LA(1);
						if ( (LA44_0==COMMA) ) {
							alt44=1;
						}

						switch (alt44) {
						case 1 :
							// FromClauseASTParser.g:268:85: COMMA Identifier LPAREN expression RPAREN
							{
							COMMA125=(Token)match(input,COMMA,FOLLOW_COMMA_in_partitionedTableFunction1845); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_COMMA.add(COMMA125);

							Identifier126=(Token)match(input,Identifier,FOLLOW_Identifier_in_partitionedTableFunction1847); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_Identifier.add(Identifier126);

							LPAREN127=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_partitionedTableFunction1849); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN127);

							pushFollow(FOLLOW_expression_in_partitionedTableFunction1851);
							expression128=gHiveASTParser.expression();
							state._fsp--;
							if (state.failed) return retval;
							if ( state.backtracking==0 ) stream_expression.add(expression128.getTree());
							RPAREN129=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_partitionedTableFunction1853); if (state.failed) return retval; 
							if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN129);

							}
							break;

						default :
							break loop44;
						}
					}

					}
					break;

			}

			// FromClauseASTParser.g:269:4: ( ( RPAREN )=> ( RPAREN ) )
			// FromClauseASTParser.g:269:5: ( RPAREN )=> ( RPAREN )
			{
			// FromClauseASTParser.g:269:17: ( RPAREN )
			// FromClauseASTParser.g:269:18: RPAREN
			{
			RPAREN130=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_partitionedTableFunction1870); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN130);

			}

			}

			// FromClauseASTParser.g:269:27: ( ( Identifier )=>alias= Identifier )?
			int alt46=2;
			alt46 = dfa46.predict(input);
			switch (alt46) {
				case 1 :
					// FromClauseASTParser.g:269:28: ( Identifier )=>alias= Identifier
					{
					alias=(Token)match(input,Identifier,FOLLOW_Identifier_in_partitionedTableFunction1883); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_Identifier.add(alias);

					}
					break;

			}

			// AST REWRITE
			// elements: alias, spec, name, expression, ptfsrc
			// token labels: name, alias
			// rule labels: ptfsrc, spec, retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleTokenStream stream_name=new RewriteRuleTokenStream(adaptor,"token name",name);
			RewriteRuleTokenStream stream_alias=new RewriteRuleTokenStream(adaptor,"token alias",alias);
			RewriteRuleSubtreeStream stream_ptfsrc=new RewriteRuleSubtreeStream(adaptor,"rule ptfsrc",ptfsrc!=null?ptfsrc.getTree():null);
			RewriteRuleSubtreeStream stream_spec=new RewriteRuleSubtreeStream(adaptor,"rule spec",spec!=null?spec.getTree():null);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 270:4: -> ^( TOK_PTBLFUNCTION $name ( $alias)? $ptfsrc ( $spec)? ( expression )* )
			{
				// FromClauseASTParser.g:270:9: ^( TOK_PTBLFUNCTION $name ( $alias)? $ptfsrc ( $spec)? ( expression )* )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_PTBLFUNCTION, "TOK_PTBLFUNCTION"), root_1);
				adaptor.addChild(root_1, stream_name.nextNode());
				// FromClauseASTParser.g:270:35: ( $alias)?
				if ( stream_alias.hasNext() ) {
					adaptor.addChild(root_1, stream_alias.nextNode());
				}
				stream_alias.reset();

				adaptor.addChild(root_1, stream_ptfsrc.nextTree());
				// FromClauseASTParser.g:270:51: ( $spec)?
				if ( stream_spec.hasNext() ) {
					adaptor.addChild(root_1, stream_spec.nextTree());
				}
				stream_spec.reset();

				// FromClauseASTParser.g:270:57: ( expression )*
				while ( stream_expression.hasNext() ) {
					adaptor.addChild(root_1, stream_expression.nextTree());
				}
				stream_expression.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "partitionedTableFunction"


	public static class whereClause_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "whereClause"
	// FromClauseASTParser.g:275:1: whereClause : KW_WHERE searchCondition -> ^( TOK_WHERE searchCondition ) ;
	public final HiveASTParser_FromClauseASTParser.whereClause_return whereClause() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.whereClause_return retval = new HiveASTParser_FromClauseASTParser.whereClause_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_WHERE131=null;
		ParserRuleReturnScope searchCondition132 =null;

		HiveParserASTNode KW_WHERE131_tree=null;
		RewriteRuleTokenStream stream_KW_WHERE=new RewriteRuleTokenStream(adaptor,"token KW_WHERE");
		RewriteRuleSubtreeStream stream_searchCondition=new RewriteRuleSubtreeStream(adaptor,"rule searchCondition");

		 gParent.pushMsg("where clause", state); 
		try {
			// FromClauseASTParser.g:278:5: ( KW_WHERE searchCondition -> ^( TOK_WHERE searchCondition ) )
			// FromClauseASTParser.g:279:5: KW_WHERE searchCondition
			{
			KW_WHERE131=(Token)match(input,KW_WHERE,FOLLOW_KW_WHERE_in_whereClause1946); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_KW_WHERE.add(KW_WHERE131);

			pushFollow(FOLLOW_searchCondition_in_whereClause1948);
			searchCondition132=searchCondition();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_searchCondition.add(searchCondition132.getTree());
			// AST REWRITE
			// elements: searchCondition
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 279:30: -> ^( TOK_WHERE searchCondition )
			{
				// FromClauseASTParser.g:279:33: ^( TOK_WHERE searchCondition )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_WHERE, "TOK_WHERE"), root_1);
				adaptor.addChild(root_1, stream_searchCondition.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "whereClause"


	public static class searchCondition_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "searchCondition"
	// FromClauseASTParser.g:282:1: searchCondition : expression ;
	public final HiveASTParser_FromClauseASTParser.searchCondition_return searchCondition() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.searchCondition_return retval = new HiveASTParser_FromClauseASTParser.searchCondition_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		ParserRuleReturnScope expression133 =null;


		 gParent.pushMsg("search condition", state); 
		try {
			// FromClauseASTParser.g:285:5: ( expression )
			// FromClauseASTParser.g:286:5: expression
			{
			root_0 = (HiveParserASTNode)adaptor.nil();


			pushFollow(FOLLOW_expression_in_searchCondition1987);
			expression133=gHiveASTParser.expression();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) adaptor.addChild(root_0, expression133.getTree());

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "searchCondition"


	public static class valueRowConstructor_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "valueRowConstructor"
	// FromClauseASTParser.g:295:1: valueRowConstructor : expressionsInParenthesis[false] -> ^( TOK_VALUE_ROW expressionsInParenthesis ) ;
	public final HiveASTParser_FromClauseASTParser.valueRowConstructor_return valueRowConstructor() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.valueRowConstructor_return retval = new HiveASTParser_FromClauseASTParser.valueRowConstructor_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		ParserRuleReturnScope expressionsInParenthesis134 =null;

		RewriteRuleSubtreeStream stream_expressionsInParenthesis=new RewriteRuleSubtreeStream(adaptor,"rule expressionsInParenthesis");

		 gParent.pushMsg("value row constructor", state); 
		try {
			// FromClauseASTParser.g:298:5: ( expressionsInParenthesis[false] -> ^( TOK_VALUE_ROW expressionsInParenthesis ) )
			// FromClauseASTParser.g:299:5: expressionsInParenthesis[false]
			{
			pushFollow(FOLLOW_expressionsInParenthesis_in_valueRowConstructor2024);
			expressionsInParenthesis134=gHiveASTParser.expressionsInParenthesis(false);
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_expressionsInParenthesis.add(expressionsInParenthesis134.getTree());
			// AST REWRITE
			// elements: expressionsInParenthesis
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 299:37: -> ^( TOK_VALUE_ROW expressionsInParenthesis )
			{
				// FromClauseASTParser.g:299:40: ^( TOK_VALUE_ROW expressionsInParenthesis )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_VALUE_ROW, "TOK_VALUE_ROW"), root_1);
				adaptor.addChild(root_1, stream_expressionsInParenthesis.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "valueRowConstructor"


	public static class valuesTableConstructor_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "valuesTableConstructor"
	// FromClauseASTParser.g:302:1: valuesTableConstructor : valueRowConstructor ( COMMA valueRowConstructor )* -> ^( TOK_VALUES_TABLE ( valueRowConstructor )+ ) ;
	public final HiveASTParser_FromClauseASTParser.valuesTableConstructor_return valuesTableConstructor() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.valuesTableConstructor_return retval = new HiveASTParser_FromClauseASTParser.valuesTableConstructor_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token COMMA136=null;
		ParserRuleReturnScope valueRowConstructor135 =null;
		ParserRuleReturnScope valueRowConstructor137 =null;

		HiveParserASTNode COMMA136_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleSubtreeStream stream_valueRowConstructor=new RewriteRuleSubtreeStream(adaptor,"rule valueRowConstructor");

		 gParent.pushMsg("values table constructor", state); 
		try {
			// FromClauseASTParser.g:305:5: ( valueRowConstructor ( COMMA valueRowConstructor )* -> ^( TOK_VALUES_TABLE ( valueRowConstructor )+ ) )
			// FromClauseASTParser.g:306:5: valueRowConstructor ( COMMA valueRowConstructor )*
			{
			pushFollow(FOLLOW_valueRowConstructor_in_valuesTableConstructor2064);
			valueRowConstructor135=valueRowConstructor();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_valueRowConstructor.add(valueRowConstructor135.getTree());
			// FromClauseASTParser.g:306:25: ( COMMA valueRowConstructor )*
			loop47:
			while (true) {
				int alt47=2;
				int LA47_0 = input.LA(1);
				if ( (LA47_0==COMMA) ) {
					alt47=1;
				}

				switch (alt47) {
				case 1 :
					// FromClauseASTParser.g:306:26: COMMA valueRowConstructor
					{
					COMMA136=(Token)match(input,COMMA,FOLLOW_COMMA_in_valuesTableConstructor2067); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_COMMA.add(COMMA136);

					pushFollow(FOLLOW_valueRowConstructor_in_valuesTableConstructor2069);
					valueRowConstructor137=valueRowConstructor();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_valueRowConstructor.add(valueRowConstructor137.getTree());
					}
					break;

				default :
					break loop47;
				}
			}

			// AST REWRITE
			// elements: valueRowConstructor
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 306:54: -> ^( TOK_VALUES_TABLE ( valueRowConstructor )+ )
			{
				// FromClauseASTParser.g:306:57: ^( TOK_VALUES_TABLE ( valueRowConstructor )+ )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_VALUES_TABLE, "TOK_VALUES_TABLE"), root_1);
				if ( !(stream_valueRowConstructor.hasNext()) ) {
					throw new RewriteEarlyExitException();
				}
				while ( stream_valueRowConstructor.hasNext() ) {
					adaptor.addChild(root_1, stream_valueRowConstructor.nextTree());
				}
				stream_valueRowConstructor.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "valuesTableConstructor"


	public static class valuesClause_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "valuesClause"
	// FromClauseASTParser.g:314:1: valuesClause : KW_VALUES ! valuesTableConstructor ;
	public final HiveASTParser_FromClauseASTParser.valuesClause_return valuesClause() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.valuesClause_return retval = new HiveASTParser_FromClauseASTParser.valuesClause_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_VALUES138=null;
		ParserRuleReturnScope valuesTableConstructor139 =null;

		HiveParserASTNode KW_VALUES138_tree=null;

		 gParent.pushMsg("values clause", state); 
		try {
			// FromClauseASTParser.g:317:5: ( KW_VALUES ! valuesTableConstructor )
			// FromClauseASTParser.g:318:5: KW_VALUES ! valuesTableConstructor
			{
			root_0 = (HiveParserASTNode)adaptor.nil();


			KW_VALUES138=(Token)match(input,KW_VALUES,FOLLOW_KW_VALUES_in_valuesClause2113); if (state.failed) return retval;
			pushFollow(FOLLOW_valuesTableConstructor_in_valuesClause2116);
			valuesTableConstructor139=valuesTableConstructor();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) adaptor.addChild(root_0, valuesTableConstructor139.getTree());

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "valuesClause"


	public static class virtualTableSource_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "virtualTableSource"
	// FromClauseASTParser.g:325:1: virtualTableSource : LPAREN valuesClause RPAREN tableNameColList -> ^( TOK_VIRTUAL_TABLE tableNameColList valuesClause ) ;
	public final HiveASTParser_FromClauseASTParser.virtualTableSource_return virtualTableSource() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.virtualTableSource_return retval = new HiveASTParser_FromClauseASTParser.virtualTableSource_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token LPAREN140=null;
		Token RPAREN142=null;
		ParserRuleReturnScope valuesClause141 =null;
		ParserRuleReturnScope tableNameColList143 =null;

		HiveParserASTNode LPAREN140_tree=null;
		HiveParserASTNode RPAREN142_tree=null;
		RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
		RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
		RewriteRuleSubtreeStream stream_valuesClause=new RewriteRuleSubtreeStream(adaptor,"rule valuesClause");
		RewriteRuleSubtreeStream stream_tableNameColList=new RewriteRuleSubtreeStream(adaptor,"rule tableNameColList");

		 gParent.pushMsg("virtual table source", state); 
		try {
			// FromClauseASTParser.g:328:4: ( LPAREN valuesClause RPAREN tableNameColList -> ^( TOK_VIRTUAL_TABLE tableNameColList valuesClause ) )
			// FromClauseASTParser.g:329:4: LPAREN valuesClause RPAREN tableNameColList
			{
			LPAREN140=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_virtualTableSource2147); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN140);

			pushFollow(FOLLOW_valuesClause_in_virtualTableSource2149);
			valuesClause141=valuesClause();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_valuesClause.add(valuesClause141.getTree());
			RPAREN142=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_virtualTableSource2151); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN142);

			pushFollow(FOLLOW_tableNameColList_in_virtualTableSource2153);
			tableNameColList143=tableNameColList();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_tableNameColList.add(tableNameColList143.getTree());
			// AST REWRITE
			// elements: valuesClause, tableNameColList
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 329:48: -> ^( TOK_VIRTUAL_TABLE tableNameColList valuesClause )
			{
				// FromClauseASTParser.g:329:51: ^( TOK_VIRTUAL_TABLE tableNameColList valuesClause )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_VIRTUAL_TABLE, "TOK_VIRTUAL_TABLE"), root_1);
				adaptor.addChild(root_1, stream_tableNameColList.nextTree());
				adaptor.addChild(root_1, stream_valuesClause.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "virtualTableSource"


	public static class tableNameColList_return extends ParserRuleReturnScope {
		HiveParserASTNode tree;
		@Override
		public HiveParserASTNode getTree() { return tree; }
	};


	// $ANTLR start "tableNameColList"
	// FromClauseASTParser.g:335:1: tableNameColList : ( KW_AS )? identifier LPAREN identifier ( COMMA identifier )* RPAREN -> ^( TOK_VIRTUAL_TABREF ^( TOK_TABNAME identifier ) ^( TOK_COL_NAME ( identifier )+ ) ) ;
	public final HiveASTParser_FromClauseASTParser.tableNameColList_return tableNameColList() throws RecognitionException {
		HiveASTParser_FromClauseASTParser.tableNameColList_return retval = new HiveASTParser_FromClauseASTParser.tableNameColList_return();
		retval.start = input.LT(1);

		HiveParserASTNode root_0 = null;

		Token KW_AS144=null;
		Token LPAREN146=null;
		Token COMMA148=null;
		Token RPAREN150=null;
		ParserRuleReturnScope identifier145 =null;
		ParserRuleReturnScope identifier147 =null;
		ParserRuleReturnScope identifier149 =null;

		HiveParserASTNode KW_AS144_tree=null;
		HiveParserASTNode LPAREN146_tree=null;
		HiveParserASTNode COMMA148_tree=null;
		HiveParserASTNode RPAREN150_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
		RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
		RewriteRuleTokenStream stream_KW_AS=new RewriteRuleTokenStream(adaptor,"token KW_AS");
		RewriteRuleSubtreeStream stream_identifier=new RewriteRuleSubtreeStream(adaptor,"rule identifier");

		 gParent.pushMsg("from source", state); 
		try {
			// FromClauseASTParser.g:338:5: ( ( KW_AS )? identifier LPAREN identifier ( COMMA identifier )* RPAREN -> ^( TOK_VIRTUAL_TABREF ^( TOK_TABNAME identifier ) ^( TOK_COL_NAME ( identifier )+ ) ) )
			// FromClauseASTParser.g:339:5: ( KW_AS )? identifier LPAREN identifier ( COMMA identifier )* RPAREN
			{
			// FromClauseASTParser.g:339:5: ( KW_AS )?
			int alt48=2;
			int LA48_0 = input.LA(1);
			if ( (LA48_0==KW_AS) ) {
				alt48=1;
			}
			switch (alt48) {
				case 1 :
					// FromClauseASTParser.g:339:5: KW_AS
					{
					KW_AS144=(Token)match(input,KW_AS,FOLLOW_KW_AS_in_tableNameColList2194); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_KW_AS.add(KW_AS144);

					}
					break;

			}

			pushFollow(FOLLOW_identifier_in_tableNameColList2197);
			identifier145=gHiveASTParser.identifier();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_identifier.add(identifier145.getTree());
			LPAREN146=(Token)match(input,LPAREN,FOLLOW_LPAREN_in_tableNameColList2199); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_LPAREN.add(LPAREN146);

			pushFollow(FOLLOW_identifier_in_tableNameColList2201);
			identifier147=gHiveASTParser.identifier();
			state._fsp--;
			if (state.failed) return retval;
			if ( state.backtracking==0 ) stream_identifier.add(identifier147.getTree());
			// FromClauseASTParser.g:339:41: ( COMMA identifier )*
			loop49:
			while (true) {
				int alt49=2;
				int LA49_0 = input.LA(1);
				if ( (LA49_0==COMMA) ) {
					alt49=1;
				}

				switch (alt49) {
				case 1 :
					// FromClauseASTParser.g:339:42: COMMA identifier
					{
					COMMA148=(Token)match(input,COMMA,FOLLOW_COMMA_in_tableNameColList2204); if (state.failed) return retval; 
					if ( state.backtracking==0 ) stream_COMMA.add(COMMA148);

					pushFollow(FOLLOW_identifier_in_tableNameColList2206);
					identifier149=gHiveASTParser.identifier();
					state._fsp--;
					if (state.failed) return retval;
					if ( state.backtracking==0 ) stream_identifier.add(identifier149.getTree());
					}
					break;

				default :
					break loop49;
				}
			}

			RPAREN150=(Token)match(input,RPAREN,FOLLOW_RPAREN_in_tableNameColList2210); if (state.failed) return retval; 
			if ( state.backtracking==0 ) stream_RPAREN.add(RPAREN150);

			// AST REWRITE
			// elements: identifier, identifier
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			if ( state.backtracking==0 ) {
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (HiveParserASTNode)adaptor.nil();
			// 339:68: -> ^( TOK_VIRTUAL_TABREF ^( TOK_TABNAME identifier ) ^( TOK_COL_NAME ( identifier )+ ) )
			{
				// FromClauseASTParser.g:339:71: ^( TOK_VIRTUAL_TABREF ^( TOK_TABNAME identifier ) ^( TOK_COL_NAME ( identifier )+ ) )
				{
				HiveParserASTNode root_1 = (HiveParserASTNode)adaptor.nil();
				root_1 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_VIRTUAL_TABREF, "TOK_VIRTUAL_TABREF"), root_1);
				// FromClauseASTParser.g:339:92: ^( TOK_TABNAME identifier )
				{
				HiveParserASTNode root_2 = (HiveParserASTNode)adaptor.nil();
				root_2 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_TABNAME, "TOK_TABNAME"), root_2);
				adaptor.addChild(root_2, stream_identifier.nextTree());
				adaptor.addChild(root_1, root_2);
				}

				// FromClauseASTParser.g:339:118: ^( TOK_COL_NAME ( identifier )+ )
				{
				HiveParserASTNode root_2 = (HiveParserASTNode)adaptor.nil();
				root_2 = (HiveParserASTNode)adaptor.becomeRoot((HiveParserASTNode)adaptor.create(TOK_COL_NAME, "TOK_COL_NAME"), root_2);
				if ( !(stream_identifier.hasNext()) ) {
					throw new RewriteEarlyExitException();
				}
				while ( stream_identifier.hasNext() ) {
					adaptor.addChild(root_2, stream_identifier.nextTree());
				}
				stream_identifier.reset();

				adaptor.addChild(root_1, root_2);
				}

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;
			}

			}

			retval.stop = input.LT(-1);

			if ( state.backtracking==0 ) {
			retval.tree = (HiveParserASTNode)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
			if ( state.backtracking==0 ) { gParent.popMsg(state); }
		}

		catch (RecognitionException e) {
		  throw e;
		}

		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "tableNameColList"

	// $ANTLR start synpred1_FromClauseASTParser
	public final void synpred1_FromClauseASTParser_fragment() throws RecognitionException {
		// FromClauseASTParser.g:106:5: ( subQuerySource )
		// FromClauseASTParser.g:106:6: subQuerySource
		{
		pushFollow(FOLLOW_subQuerySource_in_synpred1_FromClauseASTParser397);
		subQuerySource();
		state._fsp--;
		if (state.failed) return;

		}

	}
	// $ANTLR end synpred1_FromClauseASTParser

	// $ANTLR start synpred2_FromClauseASTParser
	public final void synpred2_FromClauseASTParser_fragment() throws RecognitionException {
		// FromClauseASTParser.g:160:2: ( KW_LATERAL KW_VIEW KW_OUTER )
		// FromClauseASTParser.g:160:3: KW_LATERAL KW_VIEW KW_OUTER
		{
		match(input,KW_LATERAL,FOLLOW_KW_LATERAL_in_synpred2_FromClauseASTParser881); if (state.failed) return;

		match(input,KW_VIEW,FOLLOW_KW_VIEW_in_synpred2_FromClauseASTParser883); if (state.failed) return;

		match(input,KW_OUTER,FOLLOW_KW_OUTER_in_synpred2_FromClauseASTParser885); if (state.failed) return;

		}

	}
	// $ANTLR end synpred2_FromClauseASTParser

	// $ANTLR start synpred3_FromClauseASTParser
	public final void synpred3_FromClauseASTParser_fragment() throws RecognitionException {
		// FromClauseASTParser.g:160:102: ( COMMA )
		// FromClauseASTParser.g:160:103: COMMA
		{
		match(input,COMMA,FOLLOW_COMMA_in_synpred3_FromClauseASTParser907); if (state.failed) return;

		}

	}
	// $ANTLR end synpred3_FromClauseASTParser

	// $ANTLR start synpred4_FromClauseASTParser
	public final void synpred4_FromClauseASTParser_fragment() throws RecognitionException {
		// FromClauseASTParser.g:163:60: ( COMMA )
		// FromClauseASTParser.g:163:61: COMMA
		{
		match(input,COMMA,FOLLOW_COMMA_in_synpred4_FromClauseASTParser960); if (state.failed) return;

		}

	}
	// $ANTLR end synpred4_FromClauseASTParser

	// $ANTLR start synpred6_FromClauseASTParser
	public final void synpred6_FromClauseASTParser_fragment() throws RecognitionException {
		// FromClauseASTParser.g:268:5: ( Identifier LPAREN expression RPAREN )
		// FromClauseASTParser.g:268:6: Identifier LPAREN expression RPAREN
		{
		match(input,Identifier,FOLLOW_Identifier_in_synpred6_FromClauseASTParser1823); if (state.failed) return;

		match(input,LPAREN,FOLLOW_LPAREN_in_synpred6_FromClauseASTParser1825); if (state.failed) return;

		pushFollow(FOLLOW_expression_in_synpred6_FromClauseASTParser1827);
		gHiveASTParser.expression();
		state._fsp--;
		if (state.failed) return;

		match(input,RPAREN,FOLLOW_RPAREN_in_synpred6_FromClauseASTParser1829); if (state.failed) return;

		}

	}
	// $ANTLR end synpred6_FromClauseASTParser

	// $ANTLR start synpred8_FromClauseASTParser
	public final void synpred8_FromClauseASTParser_fragment() throws RecognitionException {
		// FromClauseASTParser.g:269:28: ( Identifier )
		// FromClauseASTParser.g:269:29: Identifier
		{
		match(input,Identifier,FOLLOW_Identifier_in_synpred8_FromClauseASTParser1876); if (state.failed) return;

		}

	}
	// $ANTLR end synpred8_FromClauseASTParser

	// Delegated rules

	public final boolean synpred2_FromClauseASTParser() {
		state.backtracking++;
		int start = input.mark();
		try {
			synpred2_FromClauseASTParser_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: "+re);
		}
		boolean success = !state.failed;
		input.rewind(start);
		state.backtracking--;
		state.failed=false;
		return success;
	}
	public final boolean synpred6_FromClauseASTParser() {
		state.backtracking++;
		int start = input.mark();
		try {
			synpred6_FromClauseASTParser_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: "+re);
		}
		boolean success = !state.failed;
		input.rewind(start);
		state.backtracking--;
		state.failed=false;
		return success;
	}
	public final boolean synpred1_FromClauseASTParser() {
		state.backtracking++;
		int start = input.mark();
		try {
			synpred1_FromClauseASTParser_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: "+re);
		}
		boolean success = !state.failed;
		input.rewind(start);
		state.backtracking--;
		state.failed=false;
		return success;
	}
	public final boolean synpred4_FromClauseASTParser() {
		state.backtracking++;
		int start = input.mark();
		try {
			synpred4_FromClauseASTParser_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: "+re);
		}
		boolean success = !state.failed;
		input.rewind(start);
		state.backtracking--;
		state.failed=false;
		return success;
	}
	public final boolean synpred3_FromClauseASTParser() {
		state.backtracking++;
		int start = input.mark();
		try {
			synpred3_FromClauseASTParser_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: "+re);
		}
		boolean success = !state.failed;
		input.rewind(start);
		state.backtracking--;
		state.failed=false;
		return success;
	}
	public final boolean synpred8_FromClauseASTParser() {
		state.backtracking++;
		int start = input.mark();
		try {
			synpred8_FromClauseASTParser_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: "+re);
		}
		boolean success = !state.failed;
		input.rewind(start);
		state.backtracking--;
		state.failed=false;
		return success;
	}


	protected DFA19 dfa19 = new DFA19(this);
	protected DFA21 dfa21 = new DFA21(this);
	protected DFA32 dfa32 = new DFA32(this);
	protected DFA36 dfa36 = new DFA36(this);
	protected DFA46 dfa46 = new DFA46(this);
	static final String DFA19_eotS =
		"\150\uffff";
	static final String DFA19_eofS =
		"\1\1\147\uffff";
	static final String DFA19_minS =
		"\1\11\2\uffff\1\30\32\uffff\2\0\110\uffff";
	static final String DFA19_maxS =
		"\1\u015c\2\uffff\1\u021e\32\uffff\2\0\110\uffff";
	static final String DFA19_acceptS =
		"\1\uffff\1\2\102\uffff\1\1\43\uffff";
	static final String DFA19_specialS =
		"\36\uffff\1\0\1\1\110\uffff}>";
	static final String[] DFA19_transitionS = {
			"\1\3\55\uffff\1\1\17\uffff\1\1\31\uffff\1\1\11\uffff\1\1\27\uffff\1\1"+
			"\3\uffff\1\1\1\uffff\1\1\7\uffff\1\1\3\uffff\1\1\1\uffff\1\1\6\uffff"+
			"\1\1\4\uffff\2\1\3\uffff\1\1\11\uffff\1\1\5\uffff\1\1\15\uffff\1\1\4"+
			"\uffff\1\1\34\uffff\1\1\14\uffff\1\1\12\uffff\1\1\14\uffff\1\1\36\uffff"+
			"\1\1\12\uffff\1\1\14\uffff\1\1\1\uffff\1\1\25\uffff\1\1",
			"",
			"",
			"\1\36\1\uffff\4\37\2\uffff\1\37\1\uffff\1\37\2\uffff\1\37\1\uffff\2"+
			"\37\5\uffff\2\37\1\uffff\2\37\2\uffff\1\37\1\uffff\4\37\1\uffff\2\37"+
			"\1\uffff\4\37\2\uffff\1\37\7\uffff\1\37\1\uffff\1\37\1\uffff\3\37\1\uffff"+
			"\2\37\1\uffff\3\37\1\uffff\4\37\1\uffff\1\37\1\uffff\1\37\1\uffff\2\37"+
			"\1\uffff\1\37\1\uffff\1\37\2\uffff\1\37\1\uffff\3\37\5\uffff\4\37\5\uffff"+
			"\2\37\3\uffff\1\37\4\uffff\2\37\3\uffff\2\37\1\uffff\3\37\6\uffff\3\37"+
			"\1\uffff\4\37\3\uffff\1\37\1\uffff\3\37\1\uffff\5\37\2\uffff\3\37\1\uffff"+
			"\1\37\1\uffff\2\37\1\uffff\1\37\1\uffff\2\37\1\uffff\1\37\1\uffff\1\37"+
			"\1\uffff\1\37\2\uffff\2\37\4\uffff\2\37\1\uffff\2\37\2\uffff\2\37\1\uffff"+
			"\1\37\3\uffff\1\37\1\uffff\1\37\1\uffff\2\37\1\uffff\1\37\1\uffff\3\37"+
			"\3\uffff\10\37\1\uffff\1\37\2\uffff\2\37\4\uffff\3\37\1\uffff\4\37\1"+
			"\uffff\5\37\1\uffff\4\37\1\uffff\7\37\1\uffff\1\37\1\uffff\3\37\2\uffff"+
			"\1\37\1\uffff\3\37\4\uffff\1\37\1\uffff\1\37\1\uffff\1\37\2\uffff\3\37"+
			"\1\uffff\2\37\2\uffff\3\37\1\uffff\1\37\1\uffff\5\37\2\uffff\1\37\2\uffff"+
			"\3\37\4\uffff\1\1\61\uffff\1\37\42\uffff\1\37\52\uffff\1\37\3\uffff\1"+
			"\37\52\uffff\1\37\3\uffff\1\37\26\uffff\1\37\4\uffff\1\37",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"\1\uffff",
			"\1\uffff",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			""
	};

	static final short[] DFA19_eot = DFA.unpackEncodedString(DFA19_eotS);
	static final short[] DFA19_eof = DFA.unpackEncodedString(DFA19_eofS);
	static final char[] DFA19_min = DFA.unpackEncodedStringToUnsignedChars(DFA19_minS);
	static final char[] DFA19_max = DFA.unpackEncodedStringToUnsignedChars(DFA19_maxS);
	static final short[] DFA19_accept = DFA.unpackEncodedString(DFA19_acceptS);
	static final short[] DFA19_special = DFA.unpackEncodedString(DFA19_specialS);
	static final short[][] DFA19_transition;

	static {
		int numStates = DFA19_transitionS.length;
		DFA19_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA19_transition[i] = DFA.unpackEncodedString(DFA19_transitionS[i]);
		}
	}

	protected class DFA19 extends DFA {

		public DFA19(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 19;
			this.eot = DFA19_eot;
			this.eof = DFA19_eof;
			this.min = DFA19_min;
			this.max = DFA19_max;
			this.accept = DFA19_accept;
			this.special = DFA19_special;
			this.transition = DFA19_transition;
		}
		@Override
		public String getDescription() {
			return "()* loopback of 160:101: ( ( COMMA )=> COMMA identifier )*";
		}
		@Override
		public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
			TokenStream input = (TokenStream)_input;
			int _s = s;
			switch ( s ) {
					case 0 : 
						int LA19_30 = input.LA(1);
						 
						int index19_30 = input.index();
						input.rewind();
						s = -1;
						if ( (synpred3_FromClauseASTParser()) ) {s = 68;}
						else if ( (true) ) {s = 1;}
						 
						input.seek(index19_30);
						if ( s>=0 ) return s;
						break;

					case 1 : 
						int LA19_31 = input.LA(1);
						 
						int index19_31 = input.index();
						input.rewind();
						s = -1;
						if ( (synpred3_FromClauseASTParser()) ) {s = 68;}
						else if ( (true) ) {s = 1;}
						 
						input.seek(index19_31);
						if ( s>=0 ) return s;
						break;
			}
			if (state.backtracking>0) {state.failed=true; return -1;}
			NoViableAltException nvae =
				new NoViableAltException(getDescription(), 19, _s, input);
			error(nvae);
			throw nvae;
		}
	}

	static final String DFA21_eotS =
		"\150\uffff";
	static final String DFA21_eofS =
		"\1\1\147\uffff";
	static final String DFA21_minS =
		"\1\11\2\uffff\1\30\32\uffff\2\0\110\uffff";
	static final String DFA21_maxS =
		"\1\u015c\2\uffff\1\u021e\32\uffff\2\0\110\uffff";
	static final String DFA21_acceptS =
		"\1\uffff\1\2\102\uffff\1\1\43\uffff";
	static final String DFA21_specialS =
		"\36\uffff\1\0\1\1\110\uffff}>";
	static final String[] DFA21_transitionS = {
			"\1\3\55\uffff\1\1\17\uffff\1\1\31\uffff\1\1\11\uffff\1\1\27\uffff\1\1"+
			"\3\uffff\1\1\1\uffff\1\1\7\uffff\1\1\3\uffff\1\1\1\uffff\1\1\6\uffff"+
			"\1\1\4\uffff\2\1\3\uffff\1\1\11\uffff\1\1\5\uffff\1\1\15\uffff\1\1\4"+
			"\uffff\1\1\34\uffff\1\1\14\uffff\1\1\12\uffff\1\1\14\uffff\1\1\36\uffff"+
			"\1\1\12\uffff\1\1\14\uffff\1\1\1\uffff\1\1\25\uffff\1\1",
			"",
			"",
			"\1\36\1\uffff\4\37\2\uffff\1\37\1\uffff\1\37\2\uffff\1\37\1\uffff\2"+
			"\37\5\uffff\2\37\1\uffff\2\37\2\uffff\1\37\1\uffff\4\37\1\uffff\2\37"+
			"\1\uffff\4\37\2\uffff\1\37\7\uffff\1\37\1\uffff\1\37\1\uffff\3\37\1\uffff"+
			"\2\37\1\uffff\3\37\1\uffff\4\37\1\uffff\1\37\1\uffff\1\37\1\uffff\2\37"+
			"\1\uffff\1\37\1\uffff\1\37\2\uffff\1\37\1\uffff\3\37\5\uffff\4\37\5\uffff"+
			"\2\37\3\uffff\1\37\4\uffff\2\37\3\uffff\2\37\1\uffff\3\37\6\uffff\3\37"+
			"\1\uffff\4\37\3\uffff\1\37\1\uffff\3\37\1\uffff\5\37\2\uffff\3\37\1\uffff"+
			"\1\37\1\uffff\2\37\1\uffff\1\37\1\uffff\2\37\1\uffff\1\37\1\uffff\1\37"+
			"\1\uffff\1\37\2\uffff\2\37\4\uffff\2\37\1\uffff\2\37\2\uffff\2\37\1\uffff"+
			"\1\37\3\uffff\1\37\1\uffff\1\37\1\uffff\2\37\1\uffff\1\37\1\uffff\3\37"+
			"\3\uffff\10\37\1\uffff\1\37\2\uffff\2\37\4\uffff\3\37\1\uffff\4\37\1"+
			"\uffff\5\37\1\uffff\4\37\1\uffff\7\37\1\uffff\1\37\1\uffff\3\37\2\uffff"+
			"\1\37\1\uffff\3\37\4\uffff\1\37\1\uffff\1\37\1\uffff\1\37\2\uffff\3\37"+
			"\1\uffff\2\37\2\uffff\3\37\1\uffff\1\37\1\uffff\5\37\2\uffff\1\37\2\uffff"+
			"\3\37\4\uffff\1\1\61\uffff\1\37\42\uffff\1\37\52\uffff\1\37\3\uffff\1"+
			"\37\52\uffff\1\37\3\uffff\1\37\26\uffff\1\37\4\uffff\1\37",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"\1\uffff",
			"\1\uffff",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			""
	};

	static final short[] DFA21_eot = DFA.unpackEncodedString(DFA21_eotS);
	static final short[] DFA21_eof = DFA.unpackEncodedString(DFA21_eofS);
	static final char[] DFA21_min = DFA.unpackEncodedStringToUnsignedChars(DFA21_minS);
	static final char[] DFA21_max = DFA.unpackEncodedStringToUnsignedChars(DFA21_maxS);
	static final short[] DFA21_accept = DFA.unpackEncodedString(DFA21_acceptS);
	static final short[] DFA21_special = DFA.unpackEncodedString(DFA21_specialS);
	static final short[][] DFA21_transition;

	static {
		int numStates = DFA21_transitionS.length;
		DFA21_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA21_transition[i] = DFA.unpackEncodedString(DFA21_transitionS[i]);
		}
	}

	protected class DFA21 extends DFA {

		public DFA21(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 21;
			this.eot = DFA21_eot;
			this.eof = DFA21_eof;
			this.min = DFA21_min;
			this.max = DFA21_max;
			this.accept = DFA21_accept;
			this.special = DFA21_special;
			this.transition = DFA21_transition;
		}
		@Override
		public String getDescription() {
			return "()* loopback of 163:59: ( ( COMMA )=> COMMA identifier )*";
		}
		@Override
		public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
			TokenStream input = (TokenStream)_input;
			int _s = s;
			switch ( s ) {
					case 0 : 
						int LA21_30 = input.LA(1);
						 
						int index21_30 = input.index();
						input.rewind();
						s = -1;
						if ( (synpred4_FromClauseASTParser()) ) {s = 68;}
						else if ( (true) ) {s = 1;}
						 
						input.seek(index21_30);
						if ( s>=0 ) return s;
						break;

					case 1 : 
						int LA21_31 = input.LA(1);
						 
						int index21_31 = input.index();
						input.rewind();
						s = -1;
						if ( (synpred4_FromClauseASTParser()) ) {s = 68;}
						else if ( (true) ) {s = 1;}
						 
						input.seek(index21_31);
						if ( s>=0 ) return s;
						break;
			}
			if (state.backtracking>0) {state.failed=true; return -1;}
			NoViableAltException nvae =
				new NoViableAltException(getDescription(), 21, _s, input);
			error(nvae);
			throw nvae;
		}
	}

	static final String DFA32_eotS =
		"\u00c2\uffff";
	static final String DFA32_eofS =
		"\1\4\1\uffff\2\1\26\uffff\3\1\u00a5\uffff";
	static final String DFA32_minS =
		"\1\11\1\uffff\2\11\26\uffff\3\11\u00a5\uffff";
	static final String DFA32_maxS =
		"\1\u021e\1\uffff\2\u015c\26\uffff\3\u015c\u00a5\uffff";
	static final String DFA32_acceptS =
		"\1\uffff\1\1\2\uffff\1\2\u00bd\uffff";
	static final String DFA32_specialS =
		"\u00c2\uffff}>";
	static final String[] DFA32_transitionS = {
			"\1\4\16\uffff\1\2\1\uffff\4\1\2\uffff\1\1\1\uffff\1\1\1\uffff\2\1\1\uffff"+
			"\2\1\5\uffff\2\1\1\uffff\2\1\2\uffff\1\1\1\uffff\1\3\3\1\1\uffff\2\1"+
			"\1\uffff\4\1\2\uffff\1\1\1\uffff\1\4\5\uffff\1\1\1\uffff\1\1\1\uffff"+
			"\3\1\1\uffff\2\1\1\uffff\3\1\1\uffff\4\1\1\uffff\1\32\1\uffff\1\1\1\uffff"+
			"\2\1\1\uffff\1\1\1\uffff\1\1\1\4\1\uffff\1\1\1\uffff\3\1\5\uffff\4\1"+
			"\5\uffff\2\1\1\uffff\1\4\1\uffff\1\1\1\uffff\1\4\1\uffff\1\4\2\1\3\uffff"+
			"\2\1\1\4\3\1\1\4\1\uffff\1\4\3\uffff\3\1\1\4\4\1\2\4\1\uffff\1\1\1\uffff"+
			"\1\34\2\1\1\uffff\5\1\1\uffff\1\4\3\1\1\uffff\1\1\1\4\2\1\1\uffff\1\1"+
			"\1\uffff\2\1\1\uffff\1\1\1\uffff\1\1\1\uffff\1\1\1\4\1\uffff\2\1\1\uffff"+
			"\1\4\2\uffff\2\1\1\uffff\2\1\1\uffff\1\4\2\1\1\uffff\1\1\3\uffff\1\1"+
			"\1\uffff\1\1\1\uffff\2\1\1\uffff\1\1\1\uffff\3\1\1\4\2\uffff\10\1\1\uffff"+
			"\1\1\1\4\1\uffff\2\1\4\uffff\3\1\1\4\4\1\1\uffff\5\1\1\uffff\1\1\1\33"+
			"\2\1\1\uffff\7\1\1\uffff\1\1\1\uffff\3\1\2\uffff\1\1\1\uffff\3\1\4\uffff"+
			"\1\1\1\uffff\1\1\1\4\1\1\2\uffff\3\1\1\uffff\2\1\1\uffff\1\4\3\1\1\uffff"+
			"\1\1\1\uffff\5\1\1\uffff\1\4\1\1\1\4\1\uffff\3\1\21\uffff\1\4\44\uffff"+
			"\1\1\42\uffff\1\1\52\uffff\1\1\3\uffff\1\1\52\uffff\1\1\3\uffff\1\1\26"+
			"\uffff\1\1\4\uffff\1\1",
			"",
			"\1\1\16\uffff\1\1\36\uffff\1\1\17\uffff\1\1\31\uffff\1\1\11\uffff\1"+
			"\1\27\uffff\1\1\3\uffff\1\1\1\uffff\1\1\7\uffff\1\1\3\uffff\1\1\1\uffff"+
			"\1\1\6\uffff\1\1\4\uffff\2\1\3\uffff\1\1\11\uffff\1\1\5\uffff\1\1\15"+
			"\uffff\1\1\4\uffff\1\1\10\uffff\1\1\23\uffff\1\1\14\uffff\1\1\12\uffff"+
			"\1\1\14\uffff\1\1\36\uffff\1\1\12\uffff\1\1\14\uffff\1\1\1\uffff\1\1"+
			"\10\uffff\1\4\14\uffff\1\1",
			"\1\1\16\uffff\1\1\27\uffff\1\4\6\uffff\1\1\17\uffff\1\1\31\uffff\1\1"+
			"\11\uffff\1\1\27\uffff\1\1\3\uffff\1\1\1\uffff\1\1\7\uffff\1\1\3\uffff"+
			"\1\1\1\uffff\1\1\6\uffff\1\1\4\uffff\2\1\3\uffff\1\1\11\uffff\1\1\5\uffff"+
			"\1\1\15\uffff\1\1\4\uffff\1\1\10\uffff\1\1\23\uffff\1\1\14\uffff\1\1"+
			"\12\uffff\1\1\14\uffff\1\1\36\uffff\1\1\12\uffff\1\1\14\uffff\1\1\1\uffff"+
			"\1\1\25\uffff\1\1",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"\1\1\16\uffff\1\1\27\uffff\1\4\6\uffff\1\1\17\uffff\1\1\31\uffff\1\1"+
			"\11\uffff\1\1\27\uffff\1\1\3\uffff\1\1\1\uffff\1\1\7\uffff\1\1\3\uffff"+
			"\1\1\1\uffff\1\1\6\uffff\1\1\4\uffff\2\1\3\uffff\1\1\11\uffff\1\1\5\uffff"+
			"\1\1\15\uffff\1\1\4\uffff\1\1\10\uffff\1\1\23\uffff\1\1\14\uffff\1\1"+
			"\12\uffff\1\1\14\uffff\1\1\36\uffff\1\1\12\uffff\1\1\14\uffff\1\1\1\uffff"+
			"\1\1\25\uffff\1\1",
			"\1\1\16\uffff\1\1\27\uffff\1\4\6\uffff\1\1\17\uffff\1\1\31\uffff\1\1"+
			"\11\uffff\1\1\27\uffff\1\1\3\uffff\1\1\1\uffff\1\1\7\uffff\1\1\3\uffff"+
			"\1\1\1\uffff\1\1\6\uffff\1\1\4\uffff\2\1\3\uffff\1\1\11\uffff\1\1\5\uffff"+
			"\1\1\15\uffff\1\1\4\uffff\1\1\10\uffff\1\1\23\uffff\1\1\14\uffff\1\1"+
			"\12\uffff\1\1\14\uffff\1\1\36\uffff\1\1\12\uffff\1\1\14\uffff\1\1\1\uffff"+
			"\1\1\25\uffff\1\1",
			"\1\1\16\uffff\1\1\36\uffff\1\1\17\uffff\1\1\31\uffff\1\1\11\uffff\1"+
			"\1\27\uffff\1\1\3\uffff\1\1\1\uffff\1\1\7\uffff\1\1\3\uffff\1\1\1\uffff"+
			"\1\1\6\uffff\1\1\4\uffff\2\1\3\uffff\1\1\11\uffff\1\1\5\uffff\1\1\15"+
			"\uffff\1\1\4\uffff\1\1\10\uffff\1\1\23\uffff\1\1\14\uffff\1\1\12\uffff"+
			"\1\1\14\uffff\1\1\36\uffff\1\1\12\uffff\1\1\14\uffff\1\1\1\uffff\1\1"+
			"\16\uffff\1\4\6\uffff\1\1",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			""
	};

	static final short[] DFA32_eot = DFA.unpackEncodedString(DFA32_eotS);
	static final short[] DFA32_eof = DFA.unpackEncodedString(DFA32_eofS);
	static final char[] DFA32_min = DFA.unpackEncodedStringToUnsignedChars(DFA32_minS);
	static final char[] DFA32_max = DFA.unpackEncodedStringToUnsignedChars(DFA32_maxS);
	static final short[] DFA32_accept = DFA.unpackEncodedString(DFA32_acceptS);
	static final short[] DFA32_special = DFA.unpackEncodedString(DFA32_specialS);
	static final short[][] DFA32_transition;

	static {
		int numStates = DFA32_transitionS.length;
		DFA32_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA32_transition[i] = DFA.unpackEncodedString(DFA32_transitionS[i]);
		}
	}

	protected class DFA32 extends DFA {

		public DFA32(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 32;
			this.eot = DFA32_eot;
			this.eof = DFA32_eof;
			this.min = DFA32_min;
			this.max = DFA32_max;
			this.accept = DFA32_accept;
			this.special = DFA32_special;
			this.transition = DFA32_transition;
		}
		@Override
		public String getDescription() {
			return "204:64: ( ( KW_AS )? alias= identifier )?";
		}
	}

	static final String DFA36_eotS =
		"\u009f\uffff";
	static final String DFA36_eofS =
		"\1\uffff\2\4\u009c\uffff";
	static final String DFA36_minS =
		"\1\30\2\11\1\30\112\uffff\1\30\120\uffff";
	static final String DFA36_maxS =
		"\4\u021e\112\uffff\1\u021e\120\uffff";
	static final String DFA36_acceptS =
		"\4\uffff\1\2\u0095\uffff\1\1\4\uffff";
	static final String DFA36_specialS =
		"\u009f\uffff}>";
	static final String[] DFA36_transitionS = {
			"\1\1\1\uffff\4\2\2\uffff\1\2\1\uffff\1\2\2\uffff\1\2\1\uffff\2\2\5\uffff"+
			"\2\2\1\uffff\2\2\2\uffff\1\2\1\uffff\4\2\1\uffff\2\2\1\uffff\4\2\2\uffff"+
			"\1\2\7\uffff\1\2\1\uffff\1\2\1\uffff\3\2\1\uffff\2\2\1\uffff\3\2\1\uffff"+
			"\4\2\1\uffff\1\2\1\uffff\1\2\1\uffff\2\2\1\uffff\1\2\1\uffff\1\2\2\uffff"+
			"\1\2\1\uffff\3\2\5\uffff\4\2\5\uffff\2\2\3\uffff\1\2\4\uffff\2\2\3\uffff"+
			"\2\2\1\uffff\3\2\6\uffff\3\2\1\uffff\4\2\3\uffff\1\2\1\uffff\3\2\1\uffff"+
			"\5\2\2\uffff\3\2\1\uffff\1\2\1\uffff\2\2\1\uffff\1\2\1\uffff\2\2\1\uffff"+
			"\1\2\1\uffff\1\2\1\uffff\1\2\2\uffff\2\2\4\uffff\2\2\1\uffff\2\2\2\uffff"+
			"\2\2\1\uffff\1\2\3\uffff\1\2\1\uffff\1\2\1\uffff\2\2\1\uffff\1\2\1\uffff"+
			"\3\2\3\uffff\10\2\1\uffff\1\2\2\uffff\2\2\4\uffff\3\2\1\uffff\4\2\1\uffff"+
			"\5\2\1\uffff\4\2\1\uffff\7\2\1\uffff\1\2\1\uffff\3\2\2\uffff\1\2\1\uffff"+
			"\3\2\4\uffff\1\2\1\uffff\1\2\1\uffff\1\2\2\uffff\3\2\1\uffff\2\2\2\uffff"+
			"\3\2\1\uffff\1\2\1\uffff\5\2\2\uffff\1\2\2\uffff\3\2\66\uffff\1\2\42"+
			"\uffff\1\2\52\uffff\1\2\3\uffff\1\2\52\uffff\1\2\3\uffff\1\2\26\uffff"+
			"\1\2\4\uffff\1\2",
			"\1\4\6\uffff\1\3\7\uffff\1\4\1\uffff\4\4\2\uffff\1\4\1\uffff\1\4\1\uffff"+
			"\2\4\1\uffff\2\4\5\uffff\2\4\1\uffff\2\4\2\uffff\1\4\1\uffff\4\4\1\uffff"+
			"\2\4\1\uffff\4\4\2\uffff\1\4\1\uffff\1\4\5\uffff\1\4\1\uffff\1\4\1\uffff"+
			"\3\4\1\uffff\2\4\1\uffff\3\4\1\uffff\4\4\1\uffff\1\4\1\uffff\4\4\1\uffff"+
			"\1\4\1\uffff\4\4\1\uffff\3\4\5\uffff\4\4\3\uffff\1\4\1\uffff\4\4\1\uffff"+
			"\1\4\1\uffff\1\4\1\uffff\4\4\1\uffff\10\4\1\uffff\1\4\1\uffff\1\4\1\uffff"+
			"\12\4\1\uffff\5\4\1\uffff\5\4\1\uffff\4\4\1\uffff\4\4\1\uffff\1\4\1\uffff"+
			"\4\4\1\uffff\1\4\1\uffff\2\4\1\uffff\2\4\1\uffff\1\4\2\uffff\2\4\1\uffff"+
			"\2\4\1\uffff\3\4\1\uffff\1\4\3\uffff\1\4\1\uffff\1\4\1\uffff\2\4\1\uffff"+
			"\1\4\1\uffff\4\4\2\uffff\10\4\1\uffff\2\4\1\uffff\2\4\2\uffff\1\4\1\uffff"+
			"\16\4\1\uffff\4\4\1\uffff\7\4\1\uffff\5\4\2\uffff\5\4\4\uffff\1\4\1\uffff"+
			"\3\4\2\uffff\6\4\1\uffff\6\4\1\uffff\5\4\1\uffff\7\4\4\uffff\1\4\14\uffff"+
			"\1\4\44\uffff\1\4\42\uffff\1\4\52\uffff\1\4\3\uffff\1\4\52\uffff\1\4"+
			"\3\uffff\1\4\26\uffff\1\4\4\uffff\1\4",
			"\1\4\6\uffff\1\116\7\uffff\1\4\1\uffff\4\4\2\uffff\1\4\1\uffff\1\4\1"+
			"\uffff\2\4\1\uffff\2\4\5\uffff\2\4\1\uffff\2\4\2\uffff\1\4\1\uffff\4"+
			"\4\1\uffff\2\4\1\uffff\4\4\2\uffff\1\4\1\uffff\1\4\5\uffff\1\4\1\uffff"+
			"\1\4\1\uffff\3\4\1\uffff\2\4\1\uffff\3\4\1\uffff\4\4\1\uffff\1\4\1\uffff"+
			"\4\4\1\uffff\1\4\1\uffff\4\4\1\uffff\3\4\5\uffff\4\4\3\uffff\1\4\1\uffff"+
			"\4\4\1\uffff\1\4\1\uffff\1\4\1\uffff\4\4\1\uffff\10\4\1\uffff\1\4\1\uffff"+
			"\1\4\1\uffff\12\4\1\uffff\5\4\1\uffff\5\4\1\uffff\4\4\1\uffff\4\4\1\uffff"+
			"\1\4\1\uffff\4\4\1\uffff\1\4\1\uffff\2\4\1\uffff\2\4\1\uffff\1\4\2\uffff"+
			"\2\4\1\uffff\2\4\1\uffff\3\4\1\uffff\1\4\3\uffff\1\4\1\uffff\1\4\1\uffff"+
			"\2\4\1\uffff\1\4\1\uffff\4\4\2\uffff\10\4\1\uffff\2\4\1\uffff\2\4\2\uffff"+
			"\1\4\1\uffff\16\4\1\uffff\4\4\1\uffff\7\4\1\uffff\5\4\2\uffff\5\4\4\uffff"+
			"\1\4\1\uffff\3\4\2\uffff\6\4\1\uffff\6\4\1\uffff\5\4\1\uffff\7\4\4\uffff"+
			"\1\4\14\uffff\1\4\44\uffff\1\4\42\uffff\1\4\52\uffff\1\4\3\uffff\1\4"+
			"\52\uffff\1\4\3\uffff\1\4\26\uffff\1\4\4\uffff\1\4",
			"\1\u009a\1\uffff\4\u009a\2\uffff\1\u009a\1\uffff\1\u009a\2\uffff\1\u009a"+
			"\1\uffff\2\u009a\5\uffff\2\u009a\1\uffff\2\u009a\2\uffff\1\u009a\1\uffff"+
			"\4\u009a\1\uffff\2\u009a\1\uffff\4\u009a\2\uffff\1\u009a\7\uffff\1\u009a"+
			"\1\uffff\1\u009a\1\uffff\3\u009a\1\uffff\2\u009a\1\uffff\3\u009a\1\uffff"+
			"\4\u009a\1\uffff\1\u009a\1\uffff\1\u009a\1\uffff\2\u009a\1\uffff\1\u009a"+
			"\1\uffff\1\u009a\2\uffff\1\u009a\1\uffff\3\u009a\5\uffff\4\u009a\5\uffff"+
			"\2\u009a\3\uffff\1\u009a\4\uffff\2\u009a\3\uffff\2\u009a\1\uffff\3\u009a"+
			"\6\uffff\3\u009a\1\uffff\4\u009a\3\uffff\1\u009a\1\uffff\3\u009a\1\uffff"+
			"\5\u009a\2\uffff\3\u009a\1\uffff\1\u009a\1\uffff\2\u009a\1\uffff\1\u009a"+
			"\1\uffff\2\u009a\1\uffff\1\u009a\1\uffff\1\u009a\1\uffff\1\u009a\2\uffff"+
			"\2\u009a\4\uffff\2\u009a\1\uffff\2\u009a\2\uffff\2\u009a\1\uffff\1\u009a"+
			"\3\uffff\1\u009a\1\uffff\1\u009a\1\uffff\2\u009a\1\uffff\1\u009a\1\uffff"+
			"\3\u009a\3\uffff\10\u009a\1\uffff\1\u009a\2\uffff\2\u009a\4\uffff\3\u009a"+
			"\1\uffff\4\u009a\1\uffff\5\u009a\1\uffff\4\u009a\1\uffff\7\u009a\1\uffff"+
			"\1\u009a\1\uffff\3\u009a\2\uffff\1\u009a\1\uffff\3\u009a\4\uffff\1\u009a"+
			"\1\uffff\1\u009a\1\uffff\1\u009a\2\uffff\3\u009a\1\uffff\2\u009a\2\uffff"+
			"\3\u009a\1\uffff\1\u009a\1\uffff\5\u009a\2\uffff\1\u009a\2\uffff\3\u009a"+
			"\25\uffff\1\4\40\uffff\1\u009a\42\uffff\1\u009a\52\uffff\1\u009a\3\uffff"+
			"\1\u009a\52\uffff\1\u009a\3\uffff\1\u009a\26\uffff\1\u009a\4\uffff\1"+
			"\u009a",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"\1\u009a\1\uffff\4\u009a\2\uffff\1\u009a\1\uffff\1\u009a\2\uffff\1\u009a"+
			"\1\uffff\2\u009a\5\uffff\2\u009a\1\uffff\2\u009a\2\uffff\1\u009a\1\uffff"+
			"\4\u009a\1\uffff\2\u009a\1\uffff\4\u009a\2\uffff\1\u009a\7\uffff\1\u009a"+
			"\1\uffff\1\u009a\1\uffff\3\u009a\1\uffff\2\u009a\1\uffff\3\u009a\1\uffff"+
			"\4\u009a\1\uffff\1\u009a\1\uffff\1\u009a\1\uffff\2\u009a\1\uffff\1\u009a"+
			"\1\uffff\1\u009a\2\uffff\1\u009a\1\uffff\3\u009a\5\uffff\4\u009a\5\uffff"+
			"\2\u009a\3\uffff\1\u009a\4\uffff\2\u009a\3\uffff\2\u009a\1\uffff\3\u009a"+
			"\6\uffff\3\u009a\1\uffff\4\u009a\3\uffff\1\u009a\1\uffff\3\u009a\1\uffff"+
			"\5\u009a\2\uffff\3\u009a\1\uffff\1\u009a\1\uffff\2\u009a\1\uffff\1\u009a"+
			"\1\uffff\2\u009a\1\uffff\1\u009a\1\uffff\1\u009a\1\uffff\1\u009a\2\uffff"+
			"\2\u009a\4\uffff\2\u009a\1\uffff\2\u009a\2\uffff\2\u009a\1\uffff\1\u009a"+
			"\3\uffff\1\u009a\1\uffff\1\u009a\1\uffff\2\u009a\1\uffff\1\u009a\1\uffff"+
			"\3\u009a\3\uffff\10\u009a\1\uffff\1\u009a\2\uffff\2\u009a\4\uffff\3\u009a"+
			"\1\uffff\4\u009a\1\uffff\5\u009a\1\uffff\4\u009a\1\uffff\7\u009a\1\uffff"+
			"\1\u009a\1\uffff\3\u009a\2\uffff\1\u009a\1\uffff\3\u009a\4\uffff\1\u009a"+
			"\1\uffff\1\u009a\1\uffff\1\u009a\2\uffff\3\u009a\1\uffff\2\u009a\2\uffff"+
			"\3\u009a\1\uffff\1\u009a\1\uffff\5\u009a\2\uffff\1\u009a\2\uffff\3\u009a"+
			"\25\uffff\1\4\40\uffff\1\u009a\42\uffff\1\u009a\52\uffff\1\u009a\3\uffff"+
			"\1\u009a\52\uffff\1\u009a\3\uffff\1\u009a\26\uffff\1\u009a\4\uffff\1"+
			"\u009a",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			""
	};

	static final short[] DFA36_eot = DFA.unpackEncodedString(DFA36_eotS);
	static final short[] DFA36_eof = DFA.unpackEncodedString(DFA36_eofS);
	static final char[] DFA36_min = DFA.unpackEncodedStringToUnsignedChars(DFA36_minS);
	static final char[] DFA36_max = DFA.unpackEncodedStringToUnsignedChars(DFA36_maxS);
	static final short[] DFA36_accept = DFA.unpackEncodedString(DFA36_acceptS);
	static final short[] DFA36_special = DFA.unpackEncodedString(DFA36_specialS);
	static final short[][] DFA36_transition;

	static {
		int numStates = DFA36_transitionS.length;
		DFA36_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA36_transition[i] = DFA.unpackEncodedString(DFA36_transitionS[i]);
		}
	}

	protected class DFA36 extends DFA {

		public DFA36(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 36;
			this.eot = DFA36_eot;
			this.eof = DFA36_eof;
			this.min = DFA36_min;
			this.max = DFA36_max;
			this.accept = DFA36_accept;
			this.special = DFA36_special;
			this.transition = DFA36_transition;
		}
		@Override
		public String getDescription() {
			return "215:1: tableName : (db= identifier DOT tab= identifier -> ^( TOK_TABNAME $db $tab) |tab= identifier -> ^( TOK_TABNAME $tab) );";
		}
	}

	static final String DFA46_eotS =
		"\100\uffff";
	static final String DFA46_eofS =
		"\1\2\1\51\76\uffff";
	static final String DFA46_minS =
		"\2\11\76\uffff";
	static final String DFA46_maxS =
		"\2\u015c\76\uffff";
	static final String DFA46_acceptS =
		"\2\uffff\1\2\36\uffff\37\1";
	static final String DFA46_specialS =
		"\1\uffff\1\0\76\uffff}>";
	static final String[] DFA46_transitionS = {
			"\1\2\16\uffff\1\1\36\uffff\1\2\17\uffff\1\2\31\uffff\1\2\11\uffff\1\2"+
			"\27\uffff\1\2\3\uffff\1\2\1\uffff\1\2\7\uffff\1\2\3\uffff\1\2\1\uffff"+
			"\1\2\6\uffff\1\2\4\uffff\2\2\3\uffff\1\2\11\uffff\1\2\5\uffff\1\2\15"+
			"\uffff\1\2\4\uffff\1\2\10\uffff\1\2\23\uffff\1\2\14\uffff\1\2\12\uffff"+
			"\1\2\14\uffff\1\2\36\uffff\1\2\12\uffff\1\2\14\uffff\1\2\1\uffff\1\2"+
			"\25\uffff\1\2",
			"\1\44\16\uffff\1\77\36\uffff\1\67\17\uffff\1\45\31\uffff\1\70\11\uffff"+
			"\1\64\27\uffff\1\50\3\uffff\1\57\1\uffff\1\60\7\uffff\1\43\3\uffff\1"+
			"\52\1\uffff\1\63\6\uffff\1\42\4\uffff\1\41\1\46\3\uffff\1\72\11\uffff"+
			"\1\54\5\uffff\1\65\15\uffff\1\74\4\uffff\1\66\10\uffff\1\76\23\uffff"+
			"\1\55\14\uffff\1\47\12\uffff\1\53\14\uffff\1\71\36\uffff\1\62\12\uffff"+
			"\1\75\14\uffff\1\56\1\uffff\1\61\10\uffff\1\2\14\uffff\1\73",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			""
	};

	static final short[] DFA46_eot = DFA.unpackEncodedString(DFA46_eotS);
	static final short[] DFA46_eof = DFA.unpackEncodedString(DFA46_eofS);
	static final char[] DFA46_min = DFA.unpackEncodedStringToUnsignedChars(DFA46_minS);
	static final char[] DFA46_max = DFA.unpackEncodedStringToUnsignedChars(DFA46_maxS);
	static final short[] DFA46_accept = DFA.unpackEncodedString(DFA46_acceptS);
	static final short[] DFA46_special = DFA.unpackEncodedString(DFA46_specialS);
	static final short[][] DFA46_transition;

	static {
		int numStates = DFA46_transitionS.length;
		DFA46_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA46_transition[i] = DFA.unpackEncodedString(DFA46_transitionS[i]);
		}
	}

	protected class DFA46 extends DFA {

		public DFA46(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 46;
			this.eot = DFA46_eot;
			this.eof = DFA46_eof;
			this.min = DFA46_min;
			this.max = DFA46_max;
			this.accept = DFA46_accept;
			this.special = DFA46_special;
			this.transition = DFA46_transition;
		}
		@Override
		public String getDescription() {
			return "269:27: ( ( Identifier )=>alias= Identifier )?";
		}
		@Override
		public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
			TokenStream input = (TokenStream)_input;
			int _s = s;
			switch ( s ) {
					case 0 : 
						int LA46_1 = input.LA(1);
						 
						int index46_1 = input.index();
						input.rewind();
						s = -1;
						if ( (LA46_1==LPAREN) ) {s = 2;}
						else if ( (LA46_1==KW_LATERAL) && (synpred8_FromClauseASTParser())) {s = 33;}
						else if ( (LA46_1==KW_JOIN) && (synpred8_FromClauseASTParser())) {s = 34;}
						else if ( (LA46_1==KW_INNER) && (synpred8_FromClauseASTParser())) {s = 35;}
						else if ( (LA46_1==COMMA) && (synpred8_FromClauseASTParser())) {s = 36;}
						else if ( (LA46_1==KW_CROSS) && (synpred8_FromClauseASTParser())) {s = 37;}
						else if ( (LA46_1==KW_LEFT) && (synpred8_FromClauseASTParser())) {s = 38;}
						else if ( (LA46_1==KW_RIGHT) && (synpred8_FromClauseASTParser())) {s = 39;}
						else if ( (LA46_1==KW_FULL) && (synpred8_FromClauseASTParser())) {s = 40;}
						else if ( (LA46_1==EOF) && (synpred8_FromClauseASTParser())) {s = 41;}
						else if ( (LA46_1==KW_INSERT) && (synpred8_FromClauseASTParser())) {s = 42;}
						else if ( (LA46_1==KW_SELECT) && (synpred8_FromClauseASTParser())) {s = 43;}
						else if ( (LA46_1==KW_MAP) && (synpred8_FromClauseASTParser())) {s = 44;}
						else if ( (LA46_1==KW_REDUCE) && (synpred8_FromClauseASTParser())) {s = 45;}
						else if ( (LA46_1==KW_WHERE) && (synpred8_FromClauseASTParser())) {s = 46;}
						else if ( (LA46_1==KW_GROUP) && (synpred8_FromClauseASTParser())) {s = 47;}
						else if ( (LA46_1==KW_HAVING) && (synpred8_FromClauseASTParser())) {s = 48;}
						else if ( (LA46_1==KW_WINDOW) && (synpred8_FromClauseASTParser())) {s = 49;}
						else if ( (LA46_1==KW_UNION) && (synpred8_FromClauseASTParser())) {s = 50;}
						else if ( (LA46_1==KW_INTERSECT) && (synpred8_FromClauseASTParser())) {s = 51;}
						else if ( (LA46_1==KW_EXCEPT) && (synpred8_FromClauseASTParser())) {s = 52;}
						else if ( (LA46_1==KW_MINUS) && (synpred8_FromClauseASTParser())) {s = 53;}
						else if ( (LA46_1==KW_ORDER) && (synpred8_FromClauseASTParser())) {s = 54;}
						else if ( (LA46_1==KW_CLUSTER) && (synpred8_FromClauseASTParser())) {s = 55;}
						else if ( (LA46_1==KW_DISTRIBUTE) && (synpred8_FromClauseASTParser())) {s = 56;}
						else if ( (LA46_1==KW_SORT) && (synpred8_FromClauseASTParser())) {s = 57;}
						else if ( (LA46_1==KW_LIMIT) && (synpred8_FromClauseASTParser())) {s = 58;}
						else if ( (LA46_1==RPAREN) && (synpred8_FromClauseASTParser())) {s = 59;}
						else if ( (LA46_1==KW_ON) && (synpred8_FromClauseASTParser())) {s = 60;}
						else if ( (LA46_1==KW_USING) && (synpred8_FromClauseASTParser())) {s = 61;}
						else if ( (LA46_1==KW_PARTITION) && (synpred8_FromClauseASTParser())) {s = 62;}
						else if ( (LA46_1==Identifier) && (synpred8_FromClauseASTParser())) {s = 63;}
						 
						input.seek(index46_1);
						if ( s>=0 ) return s;
						break;
			}
			if (state.backtracking>0) {state.failed=true; return -1;}
			NoViableAltException nvae =
				new NoViableAltException(getDescription(), 46, _s, input);
			error(nvae);
			throw nvae;
		}
	}

	public static final BitSet FOLLOW_STAR_in_tableAllColumns60 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tableName_in_tableAllColumns82 = new BitSet(new long[]{0x0000000000010000L});
	public static final BitSet FOLLOW_DOT_in_tableAllColumns84 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000100000000L});
	public static final BitSet FOLLOW_STAR_in_tableAllColumns86 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_identifier_in_tableOrColumn134 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_expression_in_expressionList173 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_COMMA_in_expressionList176 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_expression_in_expressionList178 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_identifier_in_aliasList220 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_COMMA_in_aliasList223 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_aliasList225 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_KW_FROM_in_fromClause269 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD737AA1D3AFEF7DEL,0x0000000000008727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_fromSource_in_fromClause271 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_virtualTableSource_in_fromSource310 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_uniqueJoinToken_in_fromSource323 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5966C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_uniqueJoinSource_in_fromSource326 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_COMMA_in_fromSource329 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5966C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_uniqueJoinSource_in_fromSource332 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_joinSource_in_fromSource346 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tableSource_in_atomjoinSource378 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_lateralView_in_atomjoinSource381 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_subQuerySource_in_atomjoinSource402 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_lateralView_in_atomjoinSource405 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_partitionedTableFunction_in_atomjoinSource420 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_lateralView_in_atomjoinSource423 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_LPAREN_in_atomjoinSource438 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000008727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_joinSource_in_atomjoinSource441 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_atomjoinSource443 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_atomjoinSource_in_joinSource465 = new BitSet(new long[]{0x0000000000000202L,0x0000000000000080L,0x0000001040020008L,0x0020000000000000L});
	public static final BitSet FOLLOW_joinToken_in_joinSource468 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000008727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_joinSourcePart_in_joinSource471 = new BitSet(new long[]{0x0000000000000202L,0x0000000000000080L,0x0000001040020008L,0x0020000000000040L,0x0080000000000000L});
	public static final BitSet FOLLOW_KW_ON_in_joinSource474 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_expression_in_joinSource477 = new BitSet(new long[]{0x0000000000000202L,0x0000000000000080L,0x0000001040020008L,0x0020000000000000L});
	public static final BitSet FOLLOW_KW_USING_in_joinSource483 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_columnParenthesesList_in_joinSource486 = new BitSet(new long[]{0x0000000000000202L,0x0000000000000080L,0x0000001040020008L,0x0020000000000000L});
	public static final BitSet FOLLOW_tableSource_in_joinSourcePart524 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_subQuerySource_in_joinSourcePart528 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_partitionedTableFunction_in_joinSourcePart532 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_lateralView_in_joinSourcePart536 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000800000000L});
	public static final BitSet FOLLOW_KW_PRESERVE_in_uniqueJoinSource566 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_uniqueJoinTableSource_in_uniqueJoinSource569 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_uniqueJoinExpr_in_uniqueJoinSource571 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LPAREN_in_uniqueJoinExpr598 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_expressionList_in_uniqueJoinExpr601 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_uniqueJoinExpr603 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_UNIQUEJOIN_in_uniqueJoinToken631 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_JOIN_in_joinToken663 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_INNER_in_joinToken696 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L});
	public static final BitSet FOLLOW_KW_JOIN_in_joinToken698 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_COMMA_in_joinToken722 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_CROSS_in_joinToken757 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L});
	public static final BitSet FOLLOW_KW_JOIN_in_joinToken759 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_LEFT_in_joinToken783 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L,0x0000000000002000L});
	public static final BitSet FOLLOW_KW_OUTER_in_joinToken787 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L});
	public static final BitSet FOLLOW_KW_JOIN_in_joinToken791 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_RIGHT_in_joinToken803 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L,0x0000000000002000L});
	public static final BitSet FOLLOW_KW_OUTER_in_joinToken806 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L});
	public static final BitSet FOLLOW_KW_JOIN_in_joinToken810 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_FULL_in_joinToken822 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L,0x0000000000002000L});
	public static final BitSet FOLLOW_KW_OUTER_in_joinToken826 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L});
	public static final BitSet FOLLOW_KW_JOIN_in_joinToken830 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_LEFT_in_joinToken842 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000002L});
	public static final BitSet FOLLOW_KW_SEMI_in_joinToken844 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000040000000L});
	public static final BitSet FOLLOW_KW_JOIN_in_joinToken846 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_LATERAL_in_lateralView890 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x8000000000000000L});
	public static final BitSet FOLLOW_KW_VIEW_in_lateralView892 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000002000L});
	public static final BitSet FOLLOW_KW_OUTER_in_lateralView894 = new BitSet(new long[]{0xB7A6DDAD3D000000L,0x0F83A56EF76FAC27L,0xD6BDF747B85D9D23L,0xE197F8EB5166C32AL,0xD7372A1DBAFEFFDEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_function_in_lateralView896 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_tableAlias_in_lateralView898 = new BitSet(new long[]{0x0000001000000002L});
	public static final BitSet FOLLOW_KW_AS_in_lateralView901 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_lateralView903 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_COMMA_in_lateralView911 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_lateralView913 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_KW_LATERAL_in_lateralView945 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x8000000000000000L});
	public static final BitSet FOLLOW_KW_VIEW_in_lateralView947 = new BitSet(new long[]{0xB7A6DDAD3D000000L,0x0F83A56EF76FAC27L,0xD6BDF747B85D9D23L,0xE197F8EB5166C32AL,0xD7372A1DBAFEFFDEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_function_in_lateralView949 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_tableAlias_in_lateralView951 = new BitSet(new long[]{0x0000001000000002L});
	public static final BitSet FOLLOW_KW_AS_in_lateralView954 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_lateralView956 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_COMMA_in_lateralView964 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_lateralView966 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_identifier_in_tableAlias1020 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_TABLESAMPLE_in_tableBucketSample1059 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_LPAREN_in_tableBucketSample1061 = new BitSet(new long[]{0x0000400000000000L});
	public static final BitSet FOLLOW_KW_BUCKET_in_tableBucketSample1063 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000200000L});
	public static final BitSet FOLLOW_Number_in_tableBucketSample1068 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000001000L});
	public static final BitSet FOLLOW_KW_OUT_in_tableBucketSample1071 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000010L});
	public static final BitSet FOLLOW_KW_OF_in_tableBucketSample1073 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000200000L});
	public static final BitSet FOLLOW_Number_in_tableBucketSample1078 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000040L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_KW_ON_in_tableBucketSample1082 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_expression_in_tableBucketSample1086 = new BitSet(new long[]{0x0000000000000200L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_COMMA_in_tableBucketSample1089 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_expression_in_tableBucketSample1093 = new BitSet(new long[]{0x0000000000000200L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_tableBucketSample1099 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_TABLESAMPLE_in_splitSample1146 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_LPAREN_in_splitSample1148 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000200000L});
	public static final BitSet FOLLOW_Number_in_splitSample1154 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x1000000000800000L});
	public static final BitSet FOLLOW_KW_PERCENT_in_splitSample1160 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_KW_ROWS_in_splitSample1162 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_splitSample1165 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_TABLESAMPLE_in_splitSample1209 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_LPAREN_in_splitSample1211 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_ByteLengthLiteral_in_splitSample1217 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_splitSample1220 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tableBucketSample_in_tableSample1266 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_splitSample_in_tableSample1274 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tableName_in_tableSource1303 = new BitSet(new long[]{0xB7A6C1B53D000002L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3EFEF7DEL,0x0000000000008727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_tableProperties_in_tableSource1307 = new BitSet(new long[]{0xB7A6C1B53D000002L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3EFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_tableSample_in_tableSource1312 = new BitSet(new long[]{0xB7A6C1B53D000002L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_KW_AS_in_tableSource1316 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_tableSource1321 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tableName_in_uniqueJoinTableSource1377 = new BitSet(new long[]{0xB7A6C1B53D000002L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3EFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_tableSample_in_uniqueJoinTableSource1381 = new BitSet(new long[]{0xB7A6C1B53D000002L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_KW_AS_in_uniqueJoinTableSource1385 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_uniqueJoinTableSource1390 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_identifier_in_tableName1446 = new BitSet(new long[]{0x0000000000010000L});
	public static final BitSet FOLLOW_DOT_in_tableName1448 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_tableName1452 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_identifier_in_tableName1482 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_identifier_in_viewName1529 = new BitSet(new long[]{0x0000000000010000L});
	public static final BitSet FOLLOW_DOT_in_viewName1531 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_viewName1537 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LPAREN_in_subQuerySource1585 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0004000000200004L,0x0000010000000000L,0x0000000000000001L,0x0000000000008080L});
	public static final BitSet FOLLOW_queryStatementExpression_in_subQuerySource1587 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_subQuerySource1589 = new BitSet(new long[]{0xB7A6C1B53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_KW_AS_in_subQuerySource1591 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_subQuerySource1594 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_partitionByClause_in_partitioningSpec1635 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000000000000L,0x0000000000000800L});
	public static final BitSet FOLLOW_orderByClause_in_partitioningSpec1637 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_orderByClause_in_partitioningSpec1656 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_distributeByClause_in_partitioningSpec1671 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000002000L});
	public static final BitSet FOLLOW_sortByClause_in_partitioningSpec1673 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_sortByClause_in_partitioningSpec1692 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_clusterByClause_in_partitioningSpec1707 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_subQuerySource_in_partitionTableFunctionSource1744 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tableSource_in_partitionTableFunctionSource1751 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_partitionedTableFunction_in_partitionTableFunctionSource1758 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_Identifier_in_partitionedTableFunction1789 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_LPAREN_in_partitionedTableFunction1791 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000040L});
	public static final BitSet FOLLOW_KW_ON_in_partitionedTableFunction1793 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000008727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_partitionTableFunctionSource_in_partitionedTableFunction1809 = new BitSet(new long[]{0x0080000001000000L,0x0000000200000000L,0x0000000000000000L,0x0000000000100800L,0x0000000000002000L,0x0000000010000000L});
	public static final BitSet FOLLOW_partitioningSpec_in_partitionedTableFunction1813 = new BitSet(new long[]{0x0000000001000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_Identifier_in_partitionedTableFunction1835 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_LPAREN_in_partitionedTableFunction1837 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_expression_in_partitionedTableFunction1839 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_partitionedTableFunction1841 = new BitSet(new long[]{0x0000000000000200L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_COMMA_in_partitionedTableFunction1845 = new BitSet(new long[]{0x0000000001000000L});
	public static final BitSet FOLLOW_Identifier_in_partitionedTableFunction1847 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_LPAREN_in_partitionedTableFunction1849 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_expression_in_partitionedTableFunction1851 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_partitionedTableFunction1853 = new BitSet(new long[]{0x0000000000000200L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_partitionedTableFunction1870 = new BitSet(new long[]{0x0000000001000002L});
	public static final BitSet FOLLOW_Identifier_in_partitionedTableFunction1883 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_WHERE_in_whereClause1946 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_searchCondition_in_whereClause1948 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_expression_in_searchCondition1987 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_expressionsInParenthesis_in_valueRowConstructor2024 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_valueRowConstructor_in_valuesTableConstructor2064 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_COMMA_in_valuesTableConstructor2067 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_valueRowConstructor_in_valuesTableConstructor2069 = new BitSet(new long[]{0x0000000000000202L});
	public static final BitSet FOLLOW_KW_VALUES_in_valuesClause2113 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_valuesTableConstructor_in_valuesClause2116 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LPAREN_in_virtualTableSource2147 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0800000000000000L});
	public static final BitSet FOLLOW_valuesClause_in_virtualTableSource2149 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_virtualTableSource2151 = new BitSet(new long[]{0xB7A6C1B53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_tableNameColList_in_virtualTableSource2153 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_AS_in_tableNameColList2194 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_tableNameColList2197 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_LPAREN_in_tableNameColList2199 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_tableNameColList2201 = new BitSet(new long[]{0x0000000000000200L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_COMMA_in_tableNameColList2204 = new BitSet(new long[]{0xB7A6C1A53D000000L,0x0783A56AF76EA027L,0xD6B9F747B81D8C23L,0xE197F8EB5166C32AL,0xD7372A1D3AFEF7DEL,0x0000000000000727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_identifier_in_tableNameColList2206 = new BitSet(new long[]{0x0000000000000200L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_tableNameColList2210 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_subQuerySource_in_synpred1_FromClauseASTParser397 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_KW_LATERAL_in_synpred2_FromClauseASTParser881 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x8000000000000000L});
	public static final BitSet FOLLOW_KW_VIEW_in_synpred2_FromClauseASTParser883 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000002000L});
	public static final BitSet FOLLOW_KW_OUTER_in_synpred2_FromClauseASTParser885 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_COMMA_in_synpred3_FromClauseASTParser907 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_COMMA_in_synpred4_FromClauseASTParser960 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_Identifier_in_synpred6_FromClauseASTParser1823 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000008000L});
	public static final BitSet FOLLOW_LPAREN_in_synpred6_FromClauseASTParser1825 = new BitSet(new long[]{0xB7BEDDAD3F001000L,0x1FB3E56EF76FAC27L,0xD6BDF747B95D9D23L,0xE197F8EB5166C32FL,0xD7372A9DBAFEFFDEL,0x0000000600E48727L,0x0000001000000002L,0x4000000000088000L,0x0000000042000004L});
	public static final BitSet FOLLOW_expression_in_synpred6_FromClauseASTParser1827 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000010000000L});
	public static final BitSet FOLLOW_RPAREN_in_synpred6_FromClauseASTParser1829 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_Identifier_in_synpred8_FromClauseASTParser1876 = new BitSet(new long[]{0x0000000000000002L});
}
