export type SearchType = "album" | "song" | "artist";

export interface UserSummary {
  id: number;
  email: string;
  displayName: string | null;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserSummary;
}

export interface CatalogItem {
  appleCatalogId: number;
  title: string;
  artistName: string;
  genre: string | null;
  releaseDate: string | null;
  trackCount: number | null;
  artworkUrl: string | null;
  price: number | null;
  appleUrl: string | null;
  alreadySaved: boolean;
}

export interface SearchResponse {
  query: string;
  type: SearchType;
  count: number;
  results: CatalogItem[];
}

export interface LibraryItem {
  id: number;
  appleCatalogId: number;
  title: string;
  artistName: string;
  genre: string | null;
  releaseDate: string | null;
  trackCount: number | null;
  artworkUrl: string | null;
  collectionPrice: number | null;
  userRating: number | null;
  userNotes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Slice {
  label: string;
  total: number;
}

export interface CumulativePoint {
  label: string;
  total: number;
  cumulative: number;
}

export interface AnalyticsSummary {
  totals: {
    albums: number;
    artists: number;
    genres: number;
    tracks: number;
    avgRating: number;
    avgTrackCount: number;
    libraryValue: number;
  };
  byGenre: Slice[];
  byDecade: Slice[];
  releasesByYear: Slice[];
  trackCountBuckets: Slice[];
  topArtists: Slice[];
  addedOverTime: CumulativePoint[];
}

export interface LibraryFilter {
  genres: string[] | null;
  artistContains: string | null;
  titleContains: string | null;
  yearFrom: number | null;
  yearTo: number | null;
  minRating: number | null;
  maxRating: number | null;
  minTracks: number | null;
  maxTracks: number | null;
}

export interface InsightResponse {
  question: string;
  interpretation: string;
  filter: LibraryFilter;
  source: "model" | "keyword-fallback";
  count: number;
  results: LibraryItem[];
}

export interface AiStatus {
  modelEnabled: boolean;
  mode: string;
}

export interface ApiErrorBody {
  timestamp?: string;
  status: number;
  error: string;
  message?: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}
