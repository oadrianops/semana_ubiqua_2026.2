/** Tipos do painel de moderação e administração, espelhando os records do backend (ADR-0003). */
import type {
  ChangeSet,
  Money,
  OfferingView,
  OpeningHours,
  Page,
  PriceUnit,
  Role,
  TaskStatus,
  TaskType,
  UserStatus,
} from '../../shared/api/types';

export type CheckinFlag = 'LOW_ACCURACY' | null;
export type RejectionReason =
  'ILLEGIBLE_PHOTO' | 'INCONSISTENT_DATA' | 'OUT_OF_MARKET' | 'PRIVACY' | 'DUPLICATE' | 'OUTDATED' | 'OTHER';
export const REJECTION_REASONS: RejectionReason[] = [
  'ILLEGIBLE_PHOTO',
  'INCONSISTENT_DATA',
  'OUT_OF_MARKET',
  'PRIVACY',
  'DUPLICATE',
  'OUTDATED',
  'OTHER',
];

export interface QueueItem {
  publicId: string;
  type: TaskType;
  marketName: string;
  stallCode: string | null;
  stallName: string | null;
  collaboratorName: string;
  createdAt: string;
  checkinFlag: CheckinFlag;
}

export interface FieldDiff {
  field: string;
  before: unknown;
  after: unknown;
}

export interface OfferingDiff {
  productSlug: string | null;
  productName: string;
  action: string;
  before: Record<string, unknown> | null;
  after: Record<string, unknown> | null;
  conflict: boolean;
}

export interface PhotoView {
  fileId: string;
  url: string;
  thumbUrl: string | null;
  role: 'STALL' | 'OFFERING' | 'SUGGESTION' | 'EVIDENCE';
  productSlug: string | null;
  duplicateCount: number;
}

export interface SubmissionDetail {
  publicId: string;
  taskId: string;
  type: TaskType;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  marketName: string;
  marketSlug: string;
  stallId: string | null;
  stallCode: string | null;
  collaboratorName: string;
  collaboratorReputation: number;
  ownSubmission: boolean;
  commission: Money;
  checkIn: {
    distanceMeters: number | null;
    accuracyMeters: number | null;
    flag: CheckinFlag;
    checkedInAt: string | null;
  } | null;
  stallDiff: FieldDiff[];
  offerings: OfferingDiff[];
  photos: PhotoView[];
  conflicts: string[];
  notes: string | null;
  changeSet: ChangeSet;
  createdAt: string;
  rejectionReason: RejectionReason | null;
  reviewComment: string | null;
}

export type ReportStatus = 'OPEN' | 'TASK_CREATED' | 'RESOLVED' | 'DISMISSED';

export interface ReportView {
  publicId: string;
  stallId: string;
  stallCode: string;
  marketSlug: string;
  marketName: string;
  productName: string | null;
  reason: 'PRODUCT_NOT_FOUND' | 'PRICE_MISMATCH' | 'STALL_CLOSED_OR_MOVED' | 'OTHER';
  comment: string | null;
  status: ReportStatus;
  createdAt: string;
}

export type ProductStatus = 'ACTIVE' | 'PENDING' | 'MERGED' | 'INACTIVE';

export interface AdminProduct {
  slug: string;
  categorySlug: string;
  defaultUnit: PriceUnit;
  seasonMonths: number[] | null;
  status: ProductStatus;
  version: number;
  translations: Record<string, { name: string; description: string | null }>;
  aliases: Record<string, string[]>;
  completeness: Record<string, boolean>;
  suggestedBy: string | null;
  imageUrl: string | null;
}

export interface AdminCategory {
  slug: string;
  icon: string;
  sortOrder: number;
  active: boolean;
  names: Record<string, string>;
}

export interface AdminSector {
  slug: string;
  sortOrder: number;
  active: boolean;
  names: Record<string, string>;
}

export interface AdminMarket {
  slug: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  radiusMeters: number;
  openingHours: OpeningHours[];
  active: boolean;
  dataVersion: number;
  version: number;
  translations: Record<string, { description: string | null; howToGetThere: string | null }>;
  sectors: AdminSector[];
  coverThumbUrl: string | null;
  coverCredit: string | null;
  coverCreditUrl: string | null;
}

export interface AdminStall {
  publicId: string;
  marketSlug: string;
  code: string;
  displayName: string | null;
  vendorName: string | null;
  showVendorName: boolean;
  sectorSlug: string | null;
  sectorName: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  consentStatus: 'GRANTED' | 'REVOKED';
  consentMethod: 'VERBAL' | 'SIGNED_FORM' | null;
  consentRecordedAt: string | null;
  lastVerifiedAt: string | null;
  version: number;
  /** Box fictício da demonstração (ADR-0018). */
  demo: boolean;
  instagram: string | null;
  photoThumbUrl: string | null;
  latitude: number | null;
  longitude: number | null;
}

/** Foto do box no painel: a principal ou da galeria (RF-03). */
export interface AdminPhoto {
  fileId: string;
  url: string;
  thumbUrl: string;
  main: boolean;
}

export interface AdminStallDetail {
  stall: AdminStall;
  offerings: OfferingView[];
  photos: AdminPhoto[];
}

export interface AdminUser {
  publicId: string;
  id: number;
  name: string;
  emailMasked: string;
  role: Role;
  status: UserStatus;
  reputation: number;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface RuleView {
  taskType: TaskType;
  amount: Money;
  validFrom: string;
  validTo: string | null;
}

export interface BalanceView {
  collaborator: AdminUser;
  balance: Money;
  hasPixKey: boolean;
}

export interface PayoutRow {
  publicId: string;
  collaboratorId: number;
  amount: Money;
  method: 'PIX' | 'TRANSFER' | 'CASH' | 'OTHER';
  paidAt: string;
}

export interface BudgetOverview {
  period: string;
  budget: Money;
  committed: Money;
  realized: Money;
  available: Money;
  hasBudget: boolean;
  sponsors: { id: number; period: string; sponsorName: string; amount: Money }[];
}

export interface MetricsOverview {
  publishedStalls: number;
  verifiedLast30DaysPct: number;
  withPhotoPct: number;
  tasksByStatus: Partial<Record<TaskStatus, number>>;
  pendingSubmissions: number;
  avgModerationHours: number | null;
  approvalRatePct: number | null;
  creditedThisMonth: Money;
  paidThisMonth: Money;
  budget: BudgetOverview;
  searchesLast7Days: number;
  searchHitRatePct: number;
  topZeroResultTerms: { term: string; count: number }[];
  searchesByLocale: Record<string, number>;
  activeCollaboratorsThisMonth: number;
  generatedAt: string;
}

export interface AuditView {
  actorUserId: number | null;
  action: string;
  entityType: string;
  entityId: string;
  before: Record<string, unknown> | null;
  after: Record<string, unknown> | null;
  requestId: string | null;
  createdAt: string;
}

export type { Page };
