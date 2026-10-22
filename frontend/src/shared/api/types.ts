/**
 * Tipos das respostas da API, espelhando os records de leitura do backend (ADR-0003). O contrato versionado fica
 * em docs/api/openapi.json; `npm run api:generate` gera src/shared/api/schema.d.ts para conferência.
 */
import type { Money } from '../lib/format';

export type { Money };

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type PriceUnit = 'KG' | 'UNIT' | 'LITER' | 'BUNCH' | 'PORTION' | 'PACKAGE' | 'BOTTLE' | 'DOZEN';
export type Availability = 'AVAILABLE' | 'OUT_OF_STOCK' | 'SEASONAL';

export interface OpeningHours {
  days: string[];
  opens: string;
  closes: string;
}

export interface MarketSummary {
  slug: string;
  name: string;
  description: string | null;
  address: string;
  latitude: number;
  longitude: number;
  geofenceRadiusMeters: number;
  openingHours: OpeningHours[];
  publishedStalls: number;
  coverUrl: string | null;
  coverThumbUrl: string | null;
  /** Autor, licença e origem da capa (fotos com licença livre — ADR-0019). */
  coverCredit: string | null;
  coverCreditUrl: string | null;
}

export interface SectorView {
  slug: string;
  name: string;
  stallCount: number;
}

export interface MarketDetail {
  slug: string;
  name: string;
  description: string | null;
  howToGetThere: string | null;
  address: string;
  latitude: number;
  longitude: number;
  geofenceRadiusMeters: number;
  openingHours: OpeningHours[];
  publishedStalls: number;
  coverUrl: string | null;
  dataVersion: number;
  sectors: SectorView[];
  coverCredit: string | null;
  coverCreditUrl: string | null;
}

export interface Ref {
  slug: string;
  name: string;
}

export interface StallSummary {
  publicId: string;
  code: string;
  name: string | null;
  sector: Ref | null;
  photoThumbUrl: string | null;
  lastVerifiedAt: string | null;
  outdated: boolean;
  offeringCount: number;
  /** Box fictício da demonstração (ADR-0018). */
  demo: boolean;
}

export interface OfferingView {
  publicId: string;
  productSlug: string;
  productName: string;
  categorySlug: string;
  price: Money | null;
  priceUnit: PriceUnit | null;
  availability: Availability;
  lastVerifiedAt: string;
  version: number;
}

export interface StallDetail {
  publicId: string;
  code: string;
  name: string | null;
  vendorName: string | null;
  market: Ref;
  sector: Ref | null;
  photoUrl: string | null;
  photoThumbUrl: string | null;
  lastVerifiedAt: string | null;
  outdated: boolean;
  version: number;
  offerings: OfferingView[];
  demo: boolean;
  /** Perfil do Instagram (só o nome, sem @). */
  instagram: string | null;
  gallery: PhotoView[];
  /** Posição do box, quando conferida em campo ("como chegar ao box"). */
  latitude: number | null;
  longitude: number | null;
}

export interface PhotoView {
  url: string;
  thumbUrl: string;
  width: number;
  height: number;
}

export interface ProductRef {
  slug: string;
  name: string;
  category: Ref;
  imageUrl: string | null;
}

export interface StallHit {
  publicId: string;
  code: string;
  name: string | null;
  sector: string | null;
  price: Money | null;
  priceUnit: PriceUnit | null;
  availability: Availability;
  lastVerifiedAt: string;
  demo: boolean;
}

export interface MarketHits {
  market: Ref;
  stalls: StallHit[];
}

export interface SearchItem {
  product: ProductRef;
  markets: MarketHits[];
}

export interface SearchResult extends Page<SearchItem> {
  query: string;
  locale: string;
  didYouMean: string[];
}

export interface Suggestion {
  slug: string;
  name: string;
  categorySlug: string;
}

export interface CategoryView {
  slug: string;
  name: string;
  icon: string;
  productCount: number;
}

export interface ProductSummary {
  slug: string;
  name: string;
  imageUrl: string | null;
  stallCount: number;
}

export interface CategoryProducts {
  category: CategoryView;
  products: ProductSummary[];
}

export interface ProductPage {
  product: ProductRef;
  description: string | null;
  seasonMonths: number[] | null;
  defaultUnit: PriceUnit;
  markets: MarketHits[];
}

export interface SnapshotOffering {
  productSlug: string;
  price: Money | null;
  priceUnit: PriceUnit | null;
  availability: Availability;
  lastVerifiedAt: string;
}

export interface SnapshotStall {
  publicId: string;
  code: string;
  name: string | null;
  vendorName: string | null;
  sectorSlug: string | null;
  sectorName: string | null;
  photoThumbUrl: string | null;
  lastVerifiedAt: string | null;
  outdated: boolean;
  offerings: SnapshotOffering[];
  /** Ausente em snapshots salvos antes da marca de exemplo. */
  demo?: boolean;
}

export interface SnapshotProduct {
  slug: string;
  name: string;
  categorySlug: string;
  imageUrl: string | null;
  terms: string[];
}

export interface MarketSnapshot {
  market: MarketDetail;
  dataVersion: number;
  locale: string;
  generatedAt: string;
  categories: Ref[];
  products: SnapshotProduct[];
  stalls: SnapshotStall[];
}

// ------------------------------------------------------------------ conta

export type Role = 'COLLABORATOR' | 'MODERATOR' | 'ADMIN';
export type UserStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'DELETION_PENDING' | 'ANONYMIZED';

export interface Profile {
  publicId: string;
  name: string;
  email: string;
  phone: string | null;
  role: Role;
  status: UserStatus;
  preferredLocale: string;
  emailVerified: boolean;
  pixKeyType: string | null;
  pixKeyMasked: string | null;
  reputationScore: number;
  pushEnabled: boolean;
  createdAt: string;
}

export interface SessionResponse {
  accessToken: string;
  expiresAt: string;
  user: Profile;
}

// ------------------------------------------------------------------ colaboração

export type TaskType = 'NEW_STALL' | 'UPDATE_STALL' | 'VERIFY_REPORT';
export type TaskStatus =
  'ON_HOLD' | 'OPEN' | 'ACCEPTED' | 'CHECKED_IN' | 'SUBMITTED' | 'COMPLETED' | 'CANCELLED';

export interface TaskMarket {
  slug: string;
  name: string;
  latitude: number;
  longitude: number;
  radiusMeters: number;
}

export interface TaskStall {
  publicId: string;
  code: string;
  name: string | null;
  sectorName: string | null;
  version: number;
}

export interface TaskSummary {
  publicId: string;
  type: TaskType;
  status: TaskStatus;
  market: TaskMarket;
  stall: TaskStall | null;
  suggestedSector: string | null;
  commission: Money;
  priority: number;
  publishedAt: string | null;
  distanceMeters: number | null;
}

export interface MyAssignment {
  acceptedAt: string;
  checkinDeadlineAt: string;
  checkedInAt: string | null;
  submissionDeadlineAt: string | null;
  checkinDistanceMeters: number | null;
  checkinFlag: 'LOW_ACCURACY' | null;
  outcome: 'RELEASED' | 'EXPIRED' | 'APPROVED' | 'REJECTED' | null;
}

export interface TaskDetail {
  publicId: string;
  type: TaskType;
  status: TaskStatus;
  market: TaskMarket;
  stall: TaskStall | null;
  suggestedSector: string | null;
  suggestedSectorSlug: string | null;
  instructions: string | null;
  commission: Money;
  publishedAt: string | null;
  myAssignment: MyAssignment | null;
}

export interface AvailableTasks {
  items: TaskSummary[];
  reputation: number;
  minimumReputation: number;
  canAccept: boolean;
  blockedReason: string | null;
}

export interface AcceptResult {
  taskId: string;
  status: TaskStatus;
  commission: Money;
  checkinDeadlineAt: string;
}

export interface CheckInResult {
  status: TaskStatus;
  distanceMeters: number;
  flag: 'LOW_ACCURACY' | null;
  submissionDeadlineAt: string;
}

export interface UploadResult {
  fileId: string;
  status: 'TEMP' | 'ATTACHED';
  width: number;
  height: number;
  expiresAt: string | null;
}

export interface SubmissionReceipt {
  submissionId: string;
  taskId: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  receivedAt: string;
}

export interface HistoryItem {
  taskId: string;
  type: TaskType;
  taskStatus: TaskStatus;
  marketName: string;
  stallCode: string | null;
  stallName: string | null;
  commission: Money;
  acceptedAt: string;
  submittedAt: string | null;
  outcome: MyAssignment['outcome'];
  outcomeAt: string | null;
  submissionStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | null;
  rejectionReason: string | null;
  reviewComment: string | null;
}

export interface LedgerEntryView {
  publicId: string;
  type: 'CREDIT' | 'PAYOUT' | 'REVERSAL' | 'ADJUSTMENT';
  amount: Money;
  balanceAfter: Money;
  taskId: number | null;
  description: string;
  createdAt: string;
}

export interface Statement {
  balance: Money;
  totalPaidOut: Money;
  minimumPayout: Money;
  entries: Page<LedgerEntryView>;
}

export interface ReputationView {
  score: number;
  minimumToAccept: number;
  window: number;
  approved: number;
  rejected: number;
  expired: number;
}

export interface NotificationView {
  publicId: string;
  type: string;
  messageKey: string;
  params: Record<string, unknown> | null;
  title: string;
  body: string;
  link: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationPage {
  unread: number;
  page: Page<NotificationView>;
}

// ------------------------------------------------------------------ changeset (Anexo A)

export type OfferingAction = 'ADD' | 'UPDATE' | 'CONFIRM' | 'REMOVE' | 'ADD_SUGGESTED';

export interface OfferingChange {
  action: OfferingAction;
  productSlug?: string | null;
  baseVersion?: number | null;
  referencePrice?: Money | null;
  priceUnit?: PriceUnit | null;
  availability?: Availability | null;
  suggestion?: { namePt: string; categorySlug: string; photoFileId?: string | null } | null;
}

export interface ChangeSet {
  taskType: TaskType;
  stall: {
    publicId?: string | null;
    baseVersion?: number | null;
    changes?: Record<string, string> | null;
    marketSlug?: string | null;
    sectorSlug?: string | null;
    code?: string | null;
    displayName?: string | null;
    vendorName?: string | null;
    consent?: { publish: boolean; showVendorName: boolean; method: 'VERBAL' | 'SIGNED_FORM' } | null;
  } | null;
  offerings: OfferingChange[];
  photos: {
    fileId: string;
    role: 'STALL' | 'OFFERING' | 'SUGGESTION' | 'EVIDENCE';
    productSlug?: string | null;
  }[];
  notes?: string | null;
  verification?: { problemConfirmed: boolean; comment?: string | null } | null;
}
