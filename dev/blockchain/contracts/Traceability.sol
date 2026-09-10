// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/security/ReentrancyGuardUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/security/PausableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

/**
 * @title FarmingSeasonContract
 * @dev Manages farming seasons data, including IDs, types, area, start date, and status.
 */
contract FarmingSeasonContract is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable,
    PausableUpgradeable,
    UUPSUpgradeable
{
    // ── ROLES ──────────────────────────────────────────────────────────────
    bytes32 public constant SYSTEM_WRITER_ROLE = keccak256("SYSTEM_WRITER_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    // ── STRUCTS ────────────────────────────────────────────────────────────
    struct SeasonData {
        bytes32 farmId;
        bytes32 seasonId;
        string seasonName;
        string productType;
        string variety;
        uint256 area;          // in square meters (scaled by 100)
        uint256 startDate;     // Unix timestamp
        uint256 endDate;       // Unix timestamp (0 if ongoing)
        uint8 status;          // 0=IN_PROGRESS, 1=HARVESTED, 2=CANCELLED
        uint256 createdAt;     // Block timestamp
    }

    // ── STORAGE ────────────────────────────────────────────────────────────
    mapping(bytes32 => SeasonData) public seasons;
    mapping(bytes32 => bytes32[]) public farmSeasons; // farmId → seasonId[]
    bytes32[] public allSeasonIds;

    // ── EVENTS ─────────────────────────────────────────────────────────────
    event SeasonCreated(
        bytes32 indexed farmId,
        bytes32 indexed seasonId,
        string seasonName,
        uint256 startDate
    );
    event SeasonStatusUpdated(
        bytes32 indexed seasonId,
        uint8 oldStatus,
        uint8 newStatus
    );

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address defaultAdmin, address systemWriter, address upgrader) external initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();
        __Pausable_init();
        __UUPSUpgradeable_init();

        _grantRole(DEFAULT_ADMIN_ROLE, defaultAdmin);
        _grantRole(SYSTEM_WRITER_ROLE, systemWriter);
        _grantRole(UPGRADER_ROLE, upgrader);
    }

    // ── FUNCTIONS ──────────────────────────────────────────────────────────
    function createSeason(
        bytes32 farmId,
        bytes32 seasonId,
        string calldata name,
        string calldata pType,
        string calldata variety,
        uint256 area,
        uint256 startDate
    ) external onlyRole(SYSTEM_WRITER_ROLE) nonReentrant whenNotPaused returns (uint256) {
        require(seasons[seasonId].createdAt == 0, "Season already exists");

        seasons[seasonId] = SeasonData({
            farmId: farmId,
            seasonId: seasonId,
            seasonName: name,
            productType: pType,
            variety: variety,
            area: area,
            startDate: startDate,
            endDate: 0,
            status: 0, // IN_PROGRESS
            createdAt: block.timestamp
        });

        farmSeasons[farmId].push(seasonId);
        allSeasonIds.push(seasonId);

        emit SeasonCreated(farmId, seasonId, name, startDate);
        return block.number;
    }

    function updateSeasonStatus(
        bytes32 seasonId,
        uint8 newStatus
    ) external onlyRole(SYSTEM_WRITER_ROLE) whenNotPaused {
        require(seasons[seasonId].createdAt > 0, "Season does not exist");
        require(newStatus <= 2, "Invalid status code");

        uint8 oldStatus = seasons[seasonId].status;
        seasons[seasonId].status = newStatus;

        if (newStatus == 1 || newStatus == 2) {
            seasons[seasonId].endDate = block.timestamp;
        } else {
            seasons[seasonId].endDate = 0;
        }

        emit SeasonStatusUpdated(seasonId, oldStatus, newStatus);
    }

    function getSeason(
        bytes32 seasonId
    ) external view returns (SeasonData memory) {
        require(seasons[seasonId].createdAt > 0, "Season does not exist");
        return seasons[seasonId];
    }

    function getFarmSeasons(
        bytes32 farmId
    ) external view returns (bytes32[] memory) {
        return farmSeasons[farmId];
    }

    function getSeasonCount() external view returns (uint256) {
        return allSeasonIds.length;
    }

    // Pausable functions
    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }

    // UUPS Upgrade authorization
    function _authorizeUpgrade(
        address newImplementation
    ) internal override onlyRole(UPGRADER_ROLE) {}
}


/**
 * @title FarmingProcessContract
 * @dev Records farming steps and processes associated with seasons.
 */
contract FarmingProcessContract is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable,
    PausableUpgradeable,
    UUPSUpgradeable
{
    // ── ROLES ──────────────────────────────────────────────────────────────
    bytes32 public constant SYSTEM_WRITER_ROLE = keccak256("SYSTEM_WRITER_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    // ── STRUCTS ────────────────────────────────────────────────────────────
    struct ProcessData {
        bytes32 seasonId;
        bytes32 processId;
        string processType;   // SOIL_PREP|SEEDING|FERTILIZATION|PEST_CONTROL|HARVEST
        uint256 executionDate;  // Unix timestamp
        bytes32 materialsHash;  // keccak256(abi.encode(materials JSON))
        bytes32 imagesHash;     // keccak256(abi.encode(image URLs))
        uint256 createdAt;
    }

    // ── STORAGE ────────────────────────────────────────────────────────────
    mapping(bytes32 => ProcessData) public processes;
    mapping(bytes32 => bytes32[]) public seasonProcesses; // seasonId → processId[]
    bytes32[] public allProcessIds;

    // ── EVENTS ─────────────────────────────────────────────────────────────
    event ProcessAdded(
        bytes32 indexed seasonId,
        bytes32 indexed processId,
        string processType,
        uint256 executionDate
    );

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address defaultAdmin, address systemWriter, address upgrader) external initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();
        __Pausable_init();
        __UUPSUpgradeable_init();

        _grantRole(DEFAULT_ADMIN_ROLE, defaultAdmin);
        _grantRole(SYSTEM_WRITER_ROLE, systemWriter);
        _grantRole(UPGRADER_ROLE, upgrader);
    }

    // ── FUNCTIONS ──────────────────────────────────────────────────────────
    function addProcess(
        bytes32 seasonId,
        bytes32 processId,
        string calldata pType,
        uint256 execDate,
        bytes32 matHash,
        bytes32 imgHash
    ) external onlyRole(SYSTEM_WRITER_ROLE) nonReentrant whenNotPaused returns (uint256) {
        require(processes[processId].createdAt == 0, "Process already exists");

        processes[processId] = ProcessData({
            seasonId: seasonId,
            processId: processId,
            processType: pType,
            executionDate: execDate,
            materialsHash: matHash,
            imagesHash: imgHash,
            createdAt: block.timestamp
        });

        seasonProcesses[seasonId].push(processId);
        allProcessIds.push(processId);

        emit ProcessAdded(seasonId, processId, pType, execDate);
        return block.number;
    }

    function getProcess(
        bytes32 processId
    ) external view returns (ProcessData memory) {
        require(processes[processId].createdAt > 0, "Process does not exist");
        return processes[processId];
    }

    function getSeasonProcesses(
        bytes32 seasonId
    ) external view returns (bytes32[] memory) {
        return seasonProcesses[seasonId];
    }

    function getProcessCount() external view returns (uint256) {
        return allProcessIds.length;
    }

    // Pausable functions
    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }

    // UUPS Upgrade authorization
    function _authorizeUpgrade(
        address newImplementation
    ) internal override onlyRole(UPGRADER_ROLE) {}
}


/**
 * @title ExportContract
 * @dev Records season exports, quantities, units, and qrHash verifications.
 */
contract ExportContract is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable,
    PausableUpgradeable,
    UUPSUpgradeable
{
    // ── ROLES ──────────────────────────────────────────────────────────────
    bytes32 public constant SYSTEM_WRITER_ROLE = keccak256("SYSTEM_WRITER_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    // ── STRUCTS ────────────────────────────────────────────────────────────
    struct ExportData {
        bytes32 seasonId;
        bytes32 exportId;
        uint256 quantity;      // scaled by 100
        string unit;          // "kg", "ton"
        string warehouse;
        bytes32 qrHash;        // keccak256(seasonId + exportId + quantity + timestamp)
        uint256 exportDate;
        uint256 createdAt;
    }

    // ── STORAGE ────────────────────────────────────────────────────────────
    mapping(bytes32 => ExportData) public exports;
    mapping(bytes32 => ExportData) public qrHashExports; // qrHash → ExportData
    bytes32[] public allExportIds;

    // ── EVENTS ─────────────────────────────────────────────────────────────
    event ExportRecorded(
        bytes32 indexed seasonId,
        bytes32 indexed exportId,
        uint256 quantity,
        bytes32 qrHash
    );

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(address defaultAdmin, address systemWriter, address upgrader) external initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();
        __Pausable_init();
        __UUPSUpgradeable_init();

        _grantRole(DEFAULT_ADMIN_ROLE, defaultAdmin);
        _grantRole(SYSTEM_WRITER_ROLE, systemWriter);
        _grantRole(UPGRADER_ROLE, upgrader);
    }

    // ── FUNCTIONS ──────────────────────────────────────────────────────────
    function recordExport(
        bytes32 seasonId,
        bytes32 exportId,
        uint256 quantity,
        string calldata unit,
        string calldata warehouse,
        bytes32 qrHash
    ) external onlyRole(SYSTEM_WRITER_ROLE) nonReentrant whenNotPaused returns (uint256) {
        require(exports[exportId].createdAt == 0, "Export already exists");
        require(qrHashExports[qrHash].createdAt == 0, "QR Hash already registered");

        ExportData memory data = ExportData({
            seasonId: seasonId,
            exportId: exportId,
            quantity: quantity,
            unit: unit,
            warehouse: warehouse,
            qrHash: qrHash,
            exportDate: block.timestamp,
            createdAt: block.timestamp
        });

        exports[exportId] = data;
        qrHashExports[qrHash] = data;
        allExportIds.push(exportId);

        emit ExportRecorded(seasonId, exportId, quantity, qrHash);
        return block.number;
    }

    function verifyQR(
        bytes32 qrHash
    ) external view returns (bool, ExportData memory) {
        ExportData memory data = qrHashExports[qrHash];
        if (data.createdAt == 0) {
            return (false, data);
        }
        return (true, data);
    }

    function getExport(
        bytes32 exportId
    ) external view returns (ExportData memory) {
        require(exports[exportId].createdAt > 0, "Export does not exist");
        return exports[exportId];
    }

    function getExportCount() external view returns (uint256) {
        return allExportIds.length;
    }

    // Pausable functions
    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }

    // UUPS Upgrade authorization
    function _authorizeUpgrade(
        address newImplementation
    ) internal override onlyRole(UPGRADER_ROLE) {}
}


/**
 * @title TraceabilityContract
 * @dev Ties seasons, processes, exports, and farms together for origin tracing.
 */
contract TraceabilityContract is
    Initializable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable,
    PausableUpgradeable,
    UUPSUpgradeable
{
    // ── ROLES ──────────────────────────────────────────────────────────────
    bytes32 public constant SYSTEM_WRITER_ROLE = keccak256("SYSTEM_WRITER_ROLE");
    bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

    // ── STRUCTS ────────────────────────────────────────────────────────────
    struct TraceData {
        bytes32 traceId;
        bytes32 seasonId;
        bytes32[] processIds;
        bytes32 exportId;
        bytes32 farmId;
        string farmName;
        uint256 createdAt;
    }

    // ── STORAGE ────────────────────────────────────────────────────────────
    mapping(bytes32 => TraceData) public traces;
    ExportContract public exportContract;
    bytes32[] public allTraceIds;

    // ── EVENTS ─────────────────────────────────────────────────────────────
    event TraceCreated(
        bytes32 indexed traceId,
        bytes32 indexed seasonId,
        bytes32 indexed exportId
    );
    event TraceVerified(
        bytes32 indexed traceId,
        address indexed verifier,
        uint256 timestamp
    );

    /// @custom:oz-upgrades-unsafe-allow constructor
    constructor() {
        _disableInitializers();
    }

    function initialize(
        address defaultAdmin,
        address systemWriter,
        address upgrader,
        address _exportContract
    ) external initializer {
        __AccessControl_init();
        __ReentrancyGuard_init();
        __Pausable_init();
        __UUPSUpgradeable_init();

        _grantRole(DEFAULT_ADMIN_ROLE, defaultAdmin);
        _grantRole(SYSTEM_WRITER_ROLE, systemWriter);
        _grantRole(UPGRADER_ROLE, upgrader);
        exportContract = ExportContract(_exportContract);
    }

    // ── FUNCTIONS ──────────────────────────────────────────────────────────
    function createTrace(
        bytes32 traceId,
        bytes32 seasonId,
        bytes32[] calldata pIds,
        bytes32 exportId,
        bytes32 farmId,
        string calldata farmName
    ) external onlyRole(SYSTEM_WRITER_ROLE) whenNotPaused {
        require(traces[traceId].createdAt == 0, "Trace already exists");

        traces[traceId] = TraceData({
            traceId: traceId,
            seasonId: seasonId,
            processIds: pIds,
            exportId: exportId,
            farmId: farmId,
            farmName: farmName,
            createdAt: block.timestamp
        });

        allTraceIds.push(traceId);

        emit TraceCreated(traceId, seasonId, exportId);
    }

    function getTrace(
        bytes32 traceId
    ) external view returns (TraceData memory) {
        require(traces[traceId].createdAt > 0, "Trace does not exist");
        return traces[traceId];
    }

    function verify(
        bytes32 qrHash
    ) external whenNotPaused returns (bool) {
        // Verify QR hash exists in ExportContract
        (bool exists, ) = exportContract.verifyQR(qrHash);
        require(exists, "QR hash does not exist in ExportContract");

        // Record verification event
        emit TraceVerified(qrHash, msg.sender, block.timestamp);
        return true;
    }

    function updateExportContract(
        address _exportContract
    ) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(_exportContract != address(0), "Invalid contract address");
        exportContract = ExportContract(_exportContract);
    }

    function getTraceCount() external view returns (uint256) {
        return allTraceIds.length;
    }

    // Pausable functions
    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }

    // UUPS Upgrade authorization
    function _authorizeUpgrade(
        address newImplementation
    ) internal override onlyRole(UPGRADER_ROLE) {}
}
